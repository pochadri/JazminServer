/**
 * 
 */
package jazmin.deploy.domain;

import java.io.File;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jazmin.core.Jazmin;
import jazmin.log.Logger;
import jazmin.log.LoggerFactory;
import jazmin.util.BeanUtil;
import jazmin.util.FileUtil;
import jazmin.util.JSONUtil;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @author yama
 * 6 Jan, 2015
 */
public class DeployManager {
	private static Logger logger=LoggerFactory.get(DeployManager.class);
	//
	private static Map<String,Instance>instanceMap;
	private static Map<String,Machine>machineMap;
	private static Map<String,AppPackage>packages;
	private static List<PackageDownloadInfo>downloadInfos;
	private static WatchKey key;
	private static StringBuffer actionReport=new StringBuffer();
	//
	static{
		instanceMap=new ConcurrentHashMap<String, Instance>();
		machineMap=new ConcurrentHashMap<String, Machine>();
		packages=new ConcurrentHashMap<String, AppPackage>();
		downloadInfos=Collections.synchronizedList(new LinkedList<PackageDownloadInfo>());
	}
	//
	@SuppressWarnings("rawtypes")
	public static void setup() throws Exception {
		WatchService watcher = FileSystems.getDefault().newWatchService();
		String configDir = Jazmin.environment.getString("deploy.package.path");
		if (configDir == null) {
			configDir = "./workspace/package";
		}
		Path dir = FileSystems.getDefault().getPath(configDir);
		key = dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY,
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE);
		Runnable r = () -> {
			while (true) {
				try {
					key = watcher.take();
					for (WatchEvent event : key.pollEvents()) {
						Kind kind = event.kind();
						logger.info("Event on " + event.context().toString()
								+ " is " + kind);
						reloadPackage();
					}
					key.reset();
				} catch (InterruptedException e) {
					logger.catching(e);
				}
			}
		};
		new Thread(r).start();
	}
	//
	public static void reload(){
		String configDir=Jazmin.environment.getString("deploy.config.path");
		if(configDir==null){
			configDir="./workspace/config";
		}
		try{
			reloadMachineConfig(configDir);
			reloadInstanceConfig(configDir);
			reloadPackage();
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
	}
	//
	public static void setPackageVersion(List<Instance>instances,String version){
		if(version==null||version.trim().isEmpty()){
			return;
		}
		if(!Pattern.matches("\\d\\d*\\.\\d\\d*\\.\\d\\d*",version)){
			throw new IllegalArgumentException("version format just like :1.0.0");
		}
		instances.forEach(i->i.packageVersion=(version));
	}
	//
	public static void saveInstanceConfig()throws Exception{
		String configDir=Jazmin.environment.getString("deploy.config.path");
		if(configDir==null){
			configDir="./workspace/config";
		}
		File configFile=new File(configDir,"instance.json");
		List<Instance>list=getInstances();
		Collections.sort(list,(o1,o2)->o1.priority-o2.priority);
		if(configFile.exists()){
			String result=JSON.toJSONString(
					list,
					new PropertyFilter(){
						@Override
						public boolean apply(Object arg0, String name,
								Object arg2) {
							if(name.equals("alive")||
									name.equals("machine")||
									name.equals("priority")){
								return false;
							}
							return true;
						}
					},SerializerFeature.PrettyFormat);
			FileUtil.saveContent(result, configFile);
		}
	}
	//
	private static void reloadPackage(){
		packages.clear();
		String packageDir=Jazmin.environment.getString("deploy.package.path");
		if(packageDir==null){
			packageDir="./workspace/package";
		}
		File packageFolder=new File(packageDir);
		if(packageFolder.exists()&&packageFolder.isDirectory()){
			for(File ff:packageFolder.listFiles()){
				if(ff.isFile()){
					AppPackage pkg=new AppPackage();
					String fileName=ff.getName();
					pkg.id=(fileName);
					pkg.file=(ff.getAbsolutePath());
					packages.put(pkg.id,pkg);
				}
			}	
		}
	}
	//
	public static List<PackageDownloadInfo>getPackageDownloadInfos(){
		return downloadInfos;
	}
	//
	public static void addPackageDownloadInfo(PackageDownloadInfo info){
		downloadInfos.add(info);
	}
	//
	public static void removePackageDownloadInfo(PackageDownloadInfo info){
		downloadInfos.remove(info);
	}
	//
	public static List<AppPackage>getPackages(){
		return new ArrayList<AppPackage>(packages.values());
	}
	//
	public static List<AppPackage>getPackages(String search)throws Exception{
		if(search==null||search.trim().isEmpty()){
			return new ArrayList<AppPackage>();
		}
		String queryBegin="select * from "+AppPackage.class.getName()+" where 1=1 and ";
		return BeanUtil.query(getPackages(),queryBegin+search);
	}
	//
	public static List<Instance>getInstances(String search)throws Exception{
		if(search==null||search.trim().isEmpty()){
			return new ArrayList<Instance>();
		}
		String queryBegin="select * from "+Instance.class.getName()+" where 1=1 and ";
		return BeanUtil.query(getInstances(),queryBegin+search);
	}
	//
	public static Instance getInstance(String id){
		return instanceMap.get(id);
	}
	//
	public static List<Machine>getMachines(String search){
		if(search==null||search.trim().isEmpty()){
			return new ArrayList<Machine>();
		}
		String queryBegin="select * from "+Machine.class.getName()+" where 1=1 and ";
		return BeanUtil.query(getMachines(),queryBegin+search);
	}
	//
	public static List<Machine>getMachines(){
		return new ArrayList<Machine>(machineMap.values());
	}
	//
	public static List<Instance>getInstances(){
		return new ArrayList<Instance>(instanceMap.values());
	}
	//
	private static void reloadMachineConfig(String configDir)throws Exception{
		File configFile=new File(configDir,"machine.json");
		if(configFile.exists()){
			machineMap.clear();
			logger.info("load config from:"+configFile.getAbsolutePath());
			String ss=FileUtil.getContent(configFile);
			List<Machine>machines= JSONUtil.fromJsonList(ss,Machine.class);
			machines.forEach(in->machineMap.put(in.id,in));
		}else{
			logger.warn("can not find :"+configFile);
		}
	}
	//
	private static void reloadInstanceConfig(String configDir)throws Exception{
		File configFile=new File(configDir,"instance.json");
		if(configFile.exists()){
			instanceMap.clear();
			logger.info("load config from:"+configFile.getAbsolutePath());
			String ss=FileUtil.getContent(configFile);
			List<Instance>instances= JSONUtil.fromJsonList(ss,Instance.class);
			AtomicInteger ai=new AtomicInteger();
			instances.forEach(in->{
				if(in.user==null){
					in.user=("");
				}
				if(in.password==null){
					in.password=("");
				}
				if(in.packageVersion==null){
					in.packageVersion=("1.0.0");
				}
				in.priority=(ai.incrementAndGet());
				Machine m=machineMap.get(in.machineId);
				if(m==null){
					logger.error("can not find machine {} for instance {}",in.machineId,in.id);
				}else{
					in.machine=(m);
					instanceMap.put(in.id,in);			
				}
			});
		}else{
			logger.warn("can not find :"+configFile);
		}
	}
	//
	public static String renderTemplate(String instanceName){
		Instance instance=instanceMap.get(instanceName);
		if(instance==null){
			return null;
		}
		return renderTemplate(instance,instance.app);
	}
	//
	private static String renderTemplate(Instance instance,String app){
		Velocity.init();
		VelocityContext ctx=new VelocityContext();
		ctx.put("instances",getInstances());
		ctx.put("machines",getMachines());
		ctx.put("instance", instance);
		StringWriter sw=new StringWriter();
		String templateDir=Jazmin.environment.getString("deploy.template.path");
		if(templateDir==null){
			templateDir=".";
		}
		File file=new File(templateDir+"/"+app+".vm");
		if(!file.exists()){
			return null;
		}
		Velocity.mergeTemplate(templateDir+"/"+app+".vm","UTF-8", ctx, sw);
		return sw.toString();
	}
	//
	private static boolean testPort(String host, int port) {
		try {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(host, port), 1000);
			socket.close();
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
	//
	public static void testMachine(Machine machine){
		machine.isAlive=(pingHost(machine.publicHost));
	}
	//
	//
	public static String runOnMachine(Machine m,String cmd){
		StringBuilder sb=new StringBuilder();
		sb.append("-------------------------------------------------------\n");
		sb.append("machine:"+m.id+"@"+m.privateHost+"\n");
		sb.append("cmd    :"+cmd+"\n");
		try{
			SSHUtil.execute(
					m.privateHost,
					m.sshPort,
					m.sshUser,
					m.sshPassword,
					cmd,
					(out,err)->{
						sb.append("-------------------------------------------------------\n");
						sb.append(out+"\n");
						if(err!=null&&!err.isEmpty()){
							sb.append(err+"\n");			
						}
					});
		}catch(Exception e){
			sb.append(e.getMessage()+"\n");
		}
		return sb.toString();
	}
	//
	private static boolean pingHost(String host){
		try {
			return InetAddress.getByName(host).isReachable(3000);
		} catch (Exception e) {
			return false;
		}  
	}
	//
	//
	public static void testInstance(Instance instance){
		instance.isAlive=(testPort(
					instance.machine.publicHost,
					instance.port));
	}
	//
	private static String exec(Instance instance,String cmd){
		StringBuilder sb=new StringBuilder();
		sb.append("-------------------------------------------------------\n");
		sb.append("instance:"+instance.id+"\n");
		sb.append("cmd     :"+cmd+"\n");
		try{
			Machine m=instance.machine;
			SSHUtil.execute(
					m.privateHost,
					m.sshPort,
					m.sshUser,
					m.sshPassword,
					cmd,
					(out,err)->{
						sb.append("-------------------------------------------------------\n");
						sb.append(out+"\n");
						if(err!=null&&!err.isEmpty()){
							sb.append(err+"\n");			
						}
					});
		}catch(Exception e){
			sb.append(e.getMessage()+"\n");
		}
		return sb.toString();
	}
	//
	public static void startInstance(Instance instance){
		if(instance.type.startsWith("jazmin")){
			appendActionReport(exec(instance,instance.machine.jazminHome
				+"/jazmin startbg "+instance.id));
		}
		if(instance.type.equals(Instance.TYPE_MEMCACHED)){
			appendActionReport(exec(instance,instance.machine.memcachedHome
				+"/memctl start "+instance.id));
		}
		if(instance.type.equals(Instance.TYPE_HAPROXY)){
			appendActionReport(exec(instance,instance.machine.haproxyHome
				+"/hactl start "+instance.id));
		}
	}
	//
	public static void stopInstance(Instance instance){
		if(instance.type.startsWith("jazmin")){
			appendActionReport(exec(instance,instance.machine.jazminHome
					+"/jazmin stop "+instance.id));
		}
		if(instance.type.equals(Instance.TYPE_MEMCACHED)){
			appendActionReport(exec(instance,instance.machine.memcachedHome
				+"/memctl stop "+instance.id));
		}
		if(instance.type.equals(Instance.TYPE_HAPROXY)){
			appendActionReport(exec(instance,instance.machine.haproxyHome
				+"/hactl stop "+instance.id));
		}
	}
	public static String getTailLog(Instance instance) {
		if(instance.type.startsWith("jazmin")){
			return exec(instance,
							"tail -n 100 "+
							instance.machine.jazminHome+"/log/"+
							instance.id+".log");
		}
		return "not support instance type :"+instance.type;
	}
	//
	public static String getTailLog(String instanceName){
		Instance instance=instanceMap.get(instanceName);
		if(instance==null){
			return null;
		}
		return getTailLog(instance);
	}
	/**
	 * 
	 */
	public static AppPackage getInstancePackage(String instanceId) {
		Instance ins=instanceMap.get(instanceId);
		if(ins==null){
			return null;
		}
		String suffex="";
		suffex=".jaz";
		if(ins.type.equals(Instance.TYPE_JAZMIN_WEB)){
			suffex=".war";
		}
		String packageName=ins.app+"-"+ins.packageVersion+suffex;
		return packages.get(packageName);
	}
	//
	public static AppPackage getPackage(String name){
		return packages.get(name);
	}
	//
	public static void resetActionReport(){
		actionReport=new StringBuffer();
	}
	//
	public static String actionReport(){
		return actionReport.toString();
	}
	//
	public static void appendActionReport(String content){
		actionReport.append(content+"\n");
	}
}
