/**
 * 
 */
package jazmin.server.msg;

import java.lang.reflect.Method;

/**
 * @author yama
 * 26 Dec, 2014
 */
public class ServiceStub implements Comparable<ServiceStub>{
	public String serviceId;
	public Object instance;
	public Method method;
	public boolean isAsyncService;
	public boolean isDisableResponseService;
	public boolean isContinuationService;
	
	@Override
	public int compareTo(ServiceStub o) {
		return serviceId.compareTo(o.serviceId);
	}
	//
	@Override
	public String toString() {
		return "[isAsync=" + isAsyncService
				+ ", isDisableResponse=" + isDisableResponseService
				+ ", isContinuation=" + isContinuationService + "]";
	}
}
