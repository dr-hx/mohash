package edu.ustb.sei.mde.mohash;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

final public class HashAdapter extends AdapterImpl {

	private long hashCode;
	private long finalHashCode;
	
	public long getFinalHashCode() {
		return finalHashCode;
	}

	public void setFinalHashCode(long higherOrderHashCode) {
		this.finalHashCode = higherOrderHashCode;
	}

	private int matchID;
	
	public int getMatchID() {
		return matchID;
	}

	public void setMatchID(int matchID) {
		this.matchID = matchID;
	}

	public long getLocalHashCode() {
		return hashCode;
	}

	public void setLocalHashCode(long hashCode) {
		this.hashCode = hashCode;
		this.finalHashCode = hashCode;
	}

	@Override
	public boolean isAdapterForType(Object type) {
		return type==HashAdapter.class;
	}
	
	static public void make(EObject object, long hashCode) {
		HashAdapter adapter = (HashAdapter) EcoreUtil.getExistingAdapter(object, HashAdapter.class);
		if(adapter==null) {
			adapter = new HashAdapter();
			object.eAdapters().add(adapter);
		}
		adapter.setLocalHashCode(hashCode);
	}
	
	static public long getLocalHash(EObject object) {
		HashAdapter adapter = (HashAdapter) EcoreUtil.getExistingAdapter(object, HashAdapter.class);
		if(adapter==null) return 0L;
		return adapter.getLocalHashCode();
	}
	
	static public long getHash(EObject object) {
		HashAdapter adapter = (HashAdapter) EcoreUtil.getExistingAdapter(object, HashAdapter.class);
		if(adapter==null) return 0L;
		return adapter.getFinalHashCode();
	}

}
