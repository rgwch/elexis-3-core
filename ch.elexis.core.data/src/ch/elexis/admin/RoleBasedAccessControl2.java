package ch.elexis.admin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.data.Query;
import ch.elexis.data.Role;
import ch.elexis.data.User;
import ch.rgw.tools.StringTool;

public class RoleBasedAccessControl2 extends AbstractAccessControl {
	Map<String,ACE> allACE = new HashMap<String, ACE>();
	Map<User,Set<String>> userRights = new HashMap<User, Set<String>>();
	Map<Role,Set<String>> roleRights = new HashMap<Role, Set<String>>();
	
	public RoleBasedAccessControl2(){
		
	}
	
	public void fetch(){
		for (ACE ace : ACE.getAllDefinedACElements()) {
			allACE.put(ace.getUniqueHashFromACE(), ace);
		}
		
		for (User user : new Query<User>(User.class).execute()) {
			Set<String> collectedRights = userRights.get(user);
			if (collectedRights == null) {
				collectedRights = new HashSet<String>();
			}
			for (Role role : user.getAssignedRoles()) {
				for (ACE ace : getRightsForRole(role)) {
					collectedRights.add(ace.getCanonicalName());
				}
			}
			userRights.put(user, collectedRights);
		}
		
		for (Role role : new Query<Role>(Role.class).execute()) {
			Set<String> collectedRights = roleRights.get(role);
			if (collectedRights == null) {
				collectedRights = new HashSet<String>();
			}
			for (ACE ace : getRightsForRole(role)) {
				collectedRights.add(ace.getCanonicalName());
			}
			roleRights.put(role, collectedRights);
		}
		
	}
	
	private List<ACE> getRightsForRole(Role role){
		List<ACE> ret = new LinkedList<ACE>();
		List<String> rights = role.getList(role.FLD_JOINT_RIGHTS, false);
		for (String rightID : rights) {
			if (allACE.containsKey(rightID)) {
				ret.add(allACE.get(rightID));
			}
		}
		return ret;
	}
	
	@Override
	public boolean request(ACE ace){
		return request((User) ElexisEventDispatcher.getSelected(User.class), ace);
	}
	
	@Override
	public boolean request(String canonicalName){
		if (canonicalName == null || canonicalName.length() < 1)
			return false;
		
		return request(ACE.getACEByCanonicalName(canonicalName));
	}
	
	@Override
	public boolean request(Role role, ACE ace){
		if (role == null) {
			return false;
		}
		if (ace == null) {
			return false;
		}
		if (roleRights.isEmpty()) {
			fetch();
		}
		Set<String> currentRights = roleRights.get(role);
		return currentRights == null ? false : currentRights.contains(ace);
	}
	
	@Override
	public boolean request(User user, ACE ace){
		if (ace == null)
			return false;
		
		if (user == null) {
			return false;
		}
		
		if (user.isAdministrator())
			return true;
		if (userRights.isEmpty()) {
			fetch();
		}
		Set<String> currentRights = userRights.get(user);
		if(currentRights!=null){
			if(StringTool.getIndex(currentRights.toArray(new String[0]), ace.getCanonicalName())!=-1){
				return true;
			}else{
				if(ace!=ACE.ACE_ROOT){
					return request(user,ace.getParent());
				}
			}
		}
		return false;
		
	}
	
	
	@Override
	public void grant(Role r, ACE ace){
		r.grantAccessRight(ace);
	}
	
	@Override
	public void revoke(Role r, ACE ace){
		r.revokeAccessRight(ace);
	}
	
	@Override
	public void grant(String id, ACE ace){
		Role r = Role.load(id);
		grant(r, ace);
	}
	
}
