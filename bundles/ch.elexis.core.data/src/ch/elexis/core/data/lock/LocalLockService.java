package ch.elexis.core.data.lock;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.common.InstanceStatus;
import ch.elexis.core.common.InstanceStatus.STATE;
import ch.elexis.core.constants.Preferences;
import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.constants.ElexisSystemPropertyConstants;
import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.data.status.ElexisStatus;
import ch.elexis.core.lock.ILockService;
import ch.elexis.core.lock.types.LockInfo;
import ch.elexis.core.lock.types.LockRequest;
import ch.elexis.core.lock.types.LockRequest.Type;
import ch.elexis.core.lock.types.LockResponse;
import ch.elexis.core.model.IPersistentObject;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.User;

/**
 * ILocalLockService implementation. Managing locks of PersistentObjects.</br>
 * If the environment variable <b>ELEXIS_SERVER_REST_INTERFACE_URL</b> is set a
 * connection to a remote LockService will used internal.
 * 
 * @author marco
 * 
 */
public class LocalLockService implements ILockService {

	private ILockService ils;
	private InstanceStatus inst;

	private final HashMap<String, Integer> lockCount = new HashMap<String, Integer>();
	private final HashMap<String, LockInfo> locks = new HashMap<String, LockInfo>();
	private boolean standalone = false;
	private Logger logger = LoggerFactory.getLogger(LocalLockService.class);

	/**
	 * A unique id for this instance of Elexis. Changes on every restart
	 */
	private static final UUID systemUuid = UUID.randomUUID();

	private Timer timer;

	/**
	 * Construct a new LocalLockService. Application code should access via
	 * {@link CoreHub#getLocalLockService()} and <b>NOT</b> create its own instance.
	 * 
	 */
	public LocalLockService() {
		ils = new DenyAllLockService();
		timer = new Timer();
		timer.schedule(new LockRefreshTask(), 10000, 10000);

		inst = new InstanceStatus();
		inst.setState(InstanceStatus.STATE.ACTIVE);
		inst.setUuid(getSystemUuid());
		inst.setVersion(CoreHub.readElexisBuildVersion());
		inst.setOperatingSystem(System.getProperty("os.name") + "/" + System.getProperty("os.version") + "/"
				+ System.getProperty("os.arch") + "/J" + System.getProperty("java.version"));
	}

	public void reconfigure() {
		standalone = true;
		logger.info("Operating in stand-alone mode.");
	}

	@Override
	public LockResponse releaseAllLocks() {
		if (standalone) {
			return LockResponse.OK;
		}

		List<LockInfo> lockList = new ArrayList<LockInfo>(locks.values());
		for (LockInfo lockInfo : lockList) {
			LockRequest lockRequest = new LockRequest(LockRequest.Type.RELEASE, lockInfo);
			LockResponse lr = acquireOrReleaseLocks(lockRequest);
			if (!lr.isOk()) {
				return lr;
			}
		}
		return LockResponse.OK;
	}

	@Override
	public LockResponse releaseLock(IPersistentObject po) {
		if (po == null) {
			return LockResponse.DENIED(null);
		}
		logger.debug("Releasing lock on [" + po + "]");
		return releaseLock(po.storeToString());
	}

	@Override
	public LockResponse releaseLock(LockInfo lockInfo) {
		if (lockInfo.getElementStoreToString() == null) {
			return LockResponse.DENIED(null);
		}
		logger.debug("Releasing lock on [" + lockInfo.getElementStoreToString() + "]");
		return releaseLock(lockInfo.getElementStoreToString());
	}

	private LockResponse releaseLock(String storeToString) {
		User user = (User) ElexisEventDispatcher.getSelected(User.class);
		LockInfo lil = new LockInfo(storeToString, user.getId(), systemUuid.toString());
		LockRequest lockRequest = new LockRequest(LockRequest.Type.RELEASE, lil);
		return acquireOrReleaseLocks(lockRequest);
	}

	@Override
	public LockResponse acquireLockBlocking(IPersistentObject po, int secTimeout, IProgressMonitor monitor) {
		if (po == null) {
			return LockResponse.DENIED(null);
		}
		if (monitor != null) {
			monitor.beginTask("Acquiring Lock for [" + po.getLabel() + "]", (secTimeout * 10) + 1);
		}
		logger.debug("Acquiring lock blocking on [" + po + "]");
		String storeToString = po.storeToString();

		LockResponse response = acquireLock(storeToString);
		int sleptMilli = 0;
		while (!response.isOk()) {
			if (response.getStatus() == LockResponse.Status.DENIED_PERMANENT) {
				return response;
			}

			try {
				Thread.sleep(100);
				sleptMilli += 100;
				response = acquireLock(storeToString);
				if (sleptMilli > (secTimeout * 1000)) {
					return response;
				}
				// update monitor
				if (monitor != null) {
					monitor.worked(1);
					if (monitor.isCanceled()) {
						return LockResponse.DENIED(response.getLockInfo());
					}
				}
			} catch (InterruptedException e) {
				// ignore and keep trying
			}
		}
		return response;
	}

	@Override
	public LockResponse acquireLock(IPersistentObject po) {
		if (po == null) {
			return LockResponse.DENIED(null);
		}
		logger.debug("Acquiring lock on [" + po + "]");
		LockResponse lr = acquireLock(po.storeToString());

		if (lr.getStatus() == LockResponse.Status.ERROR) {
			logger.warn("LockResponse ERROR");
		}

		return lr;
	}

	private LockResponse acquireLock(String storeToString) {
		if (storeToString == null) {
			return LockResponse.DENIED(null);
		}

		User user = (User) ElexisEventDispatcher.getSelected(User.class);
		LockInfo lockInfo = new LockInfo(storeToString, user.getId(), systemUuid.toString());
		LockRequest lockRequest = new LockRequest(LockRequest.Type.ACQUIRE, lockInfo);
		return acquireOrReleaseLocks(lockRequest);
	}

	public LockResponse acquireOrReleaseLocks(LockRequest lockRequest) {
		return LockResponse.OK(lockRequest.getLockInfo());
	}

	private void incrementLockCount(LockInfo lockInfo) {
		Integer count = lockCount.get(lockInfo.getElementId());
		if (count == null) {
			count = new Integer(0);
		}
		lockCount.put(lockInfo.getElementId(), ++count);
		logger.debug("Increment to " + count + " locks on " + lockInfo.getElementId());
	}

	private void decrementLockCount(LockInfo lockInfo) {
		Integer count = lockCount.get(lockInfo.getElementId());
		if (count != null) {
			lockCount.put(lockInfo.getElementId(), --count);
			logger.debug("Decrement to " + count + " locks on " + lockInfo.getElementId());
			if (count < 1) {
				lockCount.remove(lockInfo.getElementId());
			}
		}
	}

	private Integer getCurrentLockCount(LockInfo lockInfo) {
		Integer count = lockCount.get(lockInfo.getElementId());
		if (count == null) {
			count = new Integer(0);
		}
		logger.debug("Got currently " + count + " locks on " + lockInfo.getElementId());
		return count;
	}

	@Override
	public boolean isLockedLocal(IPersistentObject po) {
		if (po == null) {
			return false;
		}

		if (standalone) {
			return true;
		}
		// check local locks first
		if (locks.containsKey(po.getId())) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isLocked(IPersistentObject po) {
		if (po == null) {
			return false;
		}
		logger.debug("Checking lock on [" + po + "]");

		User user = (User) ElexisEventDispatcher.getSelected(User.class);
		LockInfo lockInfo = new LockInfo(po.storeToString(), user.getId(), systemUuid.toString());
		LockRequest lockRequest = new LockRequest(LockRequest.Type.INFO, lockInfo);

		return isLocked(lockRequest);
	}

	public boolean isLocked(LockRequest lockRequest) {
		if (lockRequest == null || lockRequest.getLockInfo().getElementId() == null) {
			return false;
		}
		return true;
	}

	@Override
	public List<LockInfo> getCopyOfAllHeldLocks() {
		Collection<LockInfo> values = locks.values();
		if (values.size() == 0) {
			return Collections.emptyList();
		}

		return new ArrayList<LockInfo>(values);
	}

	@Override
	public String getSystemUuid() {
		return systemUuid.toString();
	}

	public LockInfo getLockInfo(String storeToString) {
		String elementId = LockInfo.getElementId(storeToString);
		LockInfo lockInfo = locks.get(elementId);
		return lockInfo;
	}

	private class LockRefreshTask extends TimerTask {
		private ILockService restService;

		@Override
		public void run() {
			try {
				final String restUrl = System
						.getProperty(ElexisSystemPropertyConstants.ELEXIS_SERVER_REST_INTERFACE_URL);
				if (restUrl != null && !restUrl.isEmpty()) {
				}

				if (standalone) {
					return;
				}

			} catch (Exception e) {
				LoggerFactory.getLogger(LockRefreshTask.class).error("Execution error", e);
			}
		}

		private boolean testRestUrl(String restUrl) {
			try {
				URL url = new URL(restUrl);
				HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
				urlConn.connect();

				return (urlConn.getResponseCode() >= 200 && urlConn.getResponseCode() < 300);
			} catch (IOException e) {
				return false;
			}
		}
	}

	private class DenyAllLockService implements ILockService {


		public LockResponse acquireOrReleaseLocks(LockRequest request) {
			return LockResponse.DENIED(getLockInfo(request.getLockInfo().getElementStoreToString()));
		}


		public boolean isLocked(LockRequest request) {
			return false;
		}

		public LockInfo getLockInfo(String storeToString) {
			return new LockInfo(storeToString, "LockService", "DenyAllLockService");
		}

		@Override
		public LockResponse acquireLock(IPersistentObject po) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public LockResponse releaseLock(IPersistentObject po) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public LockResponse releaseLock(LockInfo lockInfo) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isLocked(IPersistentObject po) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isLockedLocal(IPersistentObject po) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public LockResponse releaseAllLocks() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<LockInfo> getCopyOfAllHeldLocks() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getSystemUuid() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public LockResponse acquireLockBlocking(IPersistentObject po, int msTimeout, IProgressMonitor monitor) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Status getStatus() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void shutdown() {
			// TODO Auto-generated method stub
			
		}

	}

	@Override
	public Status getStatus() {
		if (standalone) {
			return Status.STANDALONE;
		} else if (ils == null || ils instanceof DenyAllLockService) {
			return Status.LOCAL;
		}
		return Status.REMOTE;
	}

	@Override
	public void shutdown() {
		timer.cancel();
	}
}
