package jazmin.server.console;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jazmin.core.Driver;
import jazmin.core.Jazmin;
import jazmin.core.Server;
import jazmin.core.job.JazminJob;
import jazmin.core.task.JazminTask;
import jazmin.core.thread.DispatcherCallback;
import jazmin.log.Logger;
import jazmin.log.LoggerFactory;
import jazmin.misc.io.InvokeStat;
/**
 * 
 * @author yama
 * 26 Dec, 2014
 */
public class JazminCommand extends ConsoleCommand {
    public JazminCommand() {
    	super();
    	id="jazmin";
    	desc="jazmin server ctrl command";
    	addOption("i",false,"show server information.",this::showServerInfo);
    	addOption("env",false,"show env info.",this::showEnvInfo);
    	addOption("log",false,"show all loggers",this::showLoggers);
    	addOption("log.level",true,"set log level.ALL/DEBUG/INFO/WARN/ERROR/FATAL",this::setLogLevel);
    	addOption("task",false,"show tasks",this::showTasks);
    	addOption("job",false,"show jobs",this::showJobs);  
    	addOption("job-run",true,"run job",this::runJob);  
    	addOption("task-run",true,"run task",this::runTask);  
    	addOption("driver",false,"show all drivers",this::showDrivers);  	
    	addOption("server",false,"show all servers",this::showServers);  	
    	addOption("pool-info",false,"show thread pool info",this::showThreadPoolInfo);  	
    	addOption("poll-stat",false,"show method stats",this::showThreadPoolStats);  	
    	addOption("pool-db",false,"show thread pool dashboard",this::showThreadPoolDashboard); 
    	addOption("pool-mchart",true,"show thread pool method invoke chart",this::showThreadPoolMethodChart); 
    	addOption("pool-reset",false,"reset method stats",this::resetThreadPoolStats); 
    	addOption("pool-coresize",true,"set core pool size",this::setCorePoolSize); 
    	addOption("pool-maxsize",true,"set max pool size",this::setMaxPoolSize); 
    	
    	addOption("dump",false,"dump servers and drivers",this::dump); 
        
    }
    //
    private void setMaxPoolSize(String args){
     	int count=Integer.valueOf(args);
    	Jazmin.dispatcher.setMaximumPoolSize(count);
    	out.println("max pool size set to:"+count);
    }
    //
    private void setCorePoolSize(String args){
     	int count=Integer.valueOf(args);
    	Jazmin.dispatcher.setCorePoolSize(count);
    	out.println("core pool size set to:"+count);
    	
    }
    //
    private void showServerInfo(String args){
    	String format="%-20s : %-10s\n";
		out.printf(format,"name",Jazmin.getServerName());
		out.printf(format,"version",Jazmin.VERSION);
		out.printf(format,"logLevel",LoggerFactory.getLevel());
		out.printf(format,"logFile",LoggerFactory.getFile());
		out.printf(format,"startTime",formatDate(Jazmin.getStartTime()));
		out.printf(format,"bootFile",Jazmin.getBootFile());
		out.printf(format,"applicationPackage",Jazmin.getApplicationPackage());
		out.printf(format,"appClassLoader",Jazmin.getAppClassLoader());
		out.printf(format,"serverPath",Jazmin.getServerPath());	
    }
    //
    //
    private void showEnvInfo(String args){
    	String format="%-30s : %-10s\n";
    	out.format(format,"KEY","VALUE");
    	Jazmin.environment.envs().forEach((k,v)->out.format(format,k,v));
    }
    //
    private void showLoggers(String args){
		String format="%-5s: %-100s\n";
		int i=1;
		List<Logger>loggers=LoggerFactory.getLoggers();
		out.println("total "+loggers.size()+" loggers");
		out.format(format,"#","NAME");	
		for(Logger logger:loggers){
			out.format(format,i++,logger.getName());
		};
    }
    //
    private void showJobs(String args)throws Exception{
    	String format="%-5s:%-40s %-20s %-15s %-15s %-10s\n";
		int i=1;
		List<JazminJob>jobs=Jazmin.jobStore.getJobs();
		out.println("total "+jobs.size()+" jobs");
		out.format(format,"#","NAME","CRON","LAST RUN","NEXT RUN","RUNTIMES");	
		for(JazminJob job:jobs){
			out.format(format,i++,
					cut(job.id,40),
					job.cron,
					formatDate(job.lastRunTime()),
					formatDate(job.nextRunTime()),
					job.runTimes);
		};
    }
    //
    private void showTasks(String args){
    	String format="%-5s:%-40s %-10s %-10s %-10s %-10s\n";
		int i=1;
		List<JazminTask>tasks=Jazmin.taskStore.getTasks();
		out.println("total "+tasks.size()+" tasks");
		out.format(format,"#","NAME","INITDELAY","PERIOD","TIMEUNIT","RUNTIMES");	
		for(JazminTask task:tasks){
			out.format(format,i++,
					cut(task.id,40),
					task.initialDelay,
					task.period,
					task.unit,
					task.runTimes);
		};
    }
    private void runTask(String args){
    	Jazmin.taskStore.runTask(args);
    }
    private void runJob(String args){
    	Jazmin.jobStore.runJob(args);
    }
    //
    private void setLogLevel(String logLevel){
    	LoggerFactory.setLevel(logLevel);
    }
    //
    private void showServers(String args){
		String format="%-5s : %-100s\n";
		int i=1;
		List<Server>servers=Jazmin.getServers();
		out.println("total "+servers.size()+" servers");
		out.format(format,"#","NAME");	
		for(Server server:servers){
			out.format(format,i++,server.getClass().getSimpleName());
		};
    }
    //
    private void showDrivers(String args){
		String format="%-5s : %-100s\n";
		int i=1;
		List<Driver>drivers=Jazmin.getDrivers();
		out.println("total "+drivers.size()+" drivers");
		out.format(format,"#","NAME");	
		for(Driver driver:drivers){
			out.format(format,i++,driver.getClass().getSimpleName());
		};
    }
    //
    //
    private void showThreadPoolInfo(String args){
    	String format="%-20s : %-10s\n";
		out.printf(format,"corePoolSize",Jazmin.dispatcher.getCorePoolSize());
		out.printf(format,"maxPoolSize",Jazmin.dispatcher.getMaximumPoolSize());
		out.printf(format,"keepAliveTime",Jazmin.dispatcher.getKeepAliveTime(TimeUnit.SECONDS)+" seconds");
		out.printf(format,"largestPoolSize",Jazmin.dispatcher.getLargestPoolSize());
		out.printf(format,"allowsCoreThreadTimeOut",Jazmin.dispatcher.allowsCoreThreadTimeOut());
		out.printf(format,"rejectedExecutionHandler",Jazmin.dispatcher.getRejectedExecutionHandler());
		
		int index=1;
		for(DispatcherCallback c: Jazmin.dispatcher.getGlobalDispatcherCallbacks()){
			out.printf(format,"dispatcherCallback-"+(index++),c);
		}
		//
		out.printf(format,"activeCount",Jazmin.dispatcher.getActiveCount());
		out.printf(format,"completedTaskCount",Jazmin.dispatcher.getCompletedTaskCount());
		out.printf(format,"taskCount",Jazmin.dispatcher.getTaskCount());	
    }
    //
    private void resetThreadPoolStats(String args){
    	Jazmin.dispatcher.resetInvokeStats();
    	out.println("reset thread pool method stats done");
    }
    //
    private void showThreadPoolStats(String args){
    	String format="%-5s:%-30s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n";
		int i=1;
		List<InvokeStat>stats=Jazmin.dispatcher.getInvokeStats();
		out.println("total "+stats.size()+" method stats");
		Collections.sort(stats);
		out.format(format,"#","NAME","IVC","ERR","MINT-F","MAXT-F","AVGT-F","MINT-R","MAXT-R","AVGT-R");	
		for(InvokeStat stat:stats){
			out.format(format,i++,
					cut(stat.name,30),
					stat.invokeCount,
					stat.errorCount,
					stat.minFullTime,
					stat.maxFullTime,
					stat.avgFullTime(),
					stat.minRunTime,
					stat.maxRunTime,
					stat.avgRunTime());
		};
    }
    //
    private void  showThreadPoolMethodChart(String args)throws Exception{
    	InvokeStat stat=Jazmin.dispatcher.getInvokeStat(args);
    	if(stat==null){
    		out.println("can not found method stat :"+args);
    		return;
    	}
    	String format="%-30s %-10s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n";
		
    	TerminalWriter tw=new TerminalWriter(out);
    	AsciiChart chart=new AsciiChart(200,80);
    	while(stdin.available()==0){
    		tw.cls();
    		out.println("press any key to quit.");
    		out.format(format,"NAME","IVC","ERR","MINT-F","MAXT-F",
					"AVGT-F","MINT-R","MAXT-R","AVGT-R");
    		out.format(format,
    				cut(stat.name,30),
    				stat.invokeCount,
    				stat.errorCount,
    				stat.minFullTime,
    				stat.maxFullTime,
    				stat.avgFullTime(),
    				stat.minRunTime,
    				stat.maxRunTime,
    				stat.avgRunTime());
    		printLine('=', 120);
    		int avgTime=stat.avgFullTime();
    		chart.addValue(avgTime);
    		out.println("method invoke avg time.current:"+avgTime+" ms");
    		tw.fmagenta();
    		chart.reset();
    		out.println(chart.draw());
    		tw.reset();
    		out.flush();
    		TimeUnit.SECONDS.sleep(1);
    	}
    	stdin.read();
    }
    //
    private void showThreadPoolDashboard(String args)throws Exception{
    	TerminalWriter tw=new TerminalWriter(out);
    	AsciiChart chart=new AsciiChart(200,80);
    	lastInvokeCount=Jazmin.dispatcher.getTotalInvokeCount();
    	lastSubmitCount=Jazmin.dispatcher.getTotalSubmitCount();
    	maxInvokeTps=0;
    	maxSubmitTps=0;
    	
    	while(stdin.available()==0){
    		tw.cls();
    		out.println("press any key to quit.");
    		printLine('=', 100);
    		showThreadPoolInfo(args);
    		printLine('=', 100);
    		showThreadPoolTps0(chart,tw);
    		out.flush();
    		TimeUnit.SECONDS.sleep(1);
    	}
    	stdin.read();
    }
    //
    private long lastInvokeCount=0;
    private long lastSubmitCount=0;
    
    private long maxInvokeTps=0;
    private long maxSubmitTps=0;
    
    //
    private void showThreadPoolTps0(AsciiChart chart,TerminalWriter tw){
    	String format="%-20s : %-10s\n";
		out.printf(format,"activeCount",Jazmin.dispatcher.getActiveCount());
		out.printf(format,"completedTaskCount",Jazmin.dispatcher.getCompletedTaskCount());
		out.printf(format,"taskCount",Jazmin.dispatcher.getTaskCount());	
    	//
    	long invokeCount=Jazmin.dispatcher.getTotalInvokeCount();
    	long submitCount=Jazmin.dispatcher.getTotalSubmitCount();
    	format="%-10s %-30s %-10s %-10s %-10s %-10s %-10s\n";
    	out.printf(format,
    			"TYPE",
    			"DATE",
    			"LASTCOUNT",
    			"COUNT",
    			"MAXTPS",
    			"QUEUESIZE",
    			"TPS");
    	long invokeTps=invokeCount-lastInvokeCount;
    	if(invokeTps>maxInvokeTps){
    		maxInvokeTps=invokeTps;
    	}
    	chart.addValue((int)(invokeTps));
    	out.printf(format,
    			"INVOKE",
    			formatDate(new Date()),
    			lastInvokeCount,
    			invokeCount,
    			maxInvokeTps,
    			Jazmin.dispatcher.getRequestQueueSize(),
    			invokeTps);
    	//
    	long submitTps=submitCount-lastSubmitCount;
    	if(submitTps>maxSubmitTps){
    		maxSubmitTps=submitTps;
    	}
    	out.printf(format,
    			"SUBMIT",
    			formatDate(new Date()),
    			lastSubmitCount,
    			submitCount,
    			maxSubmitTps,
    			Jazmin.dispatcher.getRequestQueueSize(),
    			submitTps);
    	
    	lastInvokeCount=invokeCount;
    	lastSubmitCount=submitCount;
    	//
    	printLine('=', 100);
		out.println("thread pool invoke tps chart. current:"+invokeTps+"/s");
		tw.fmagenta();
		chart.reset();
		out.println(chart.draw());
		tw.reset();
		
    }
    //
    private void dump(String args){
    	Jazmin.getServers().forEach(server->{
    		out.println(server.getClass().getName()+" dump info");
    		out.println(server.info());
    	});
    	Jazmin.getDrivers().forEach(driver->{
    		out.println(driver.getClass().getName()+" dump info");
    		out.println(driver.info());
    	});
    }
}
