package nl.idgis.publisher.data;

import java.util.ArrayList;
import java.util.List;

public class GenericLayer {
	public static List<String> transformUserGroupsToList(String userGroupsAsText) {
		String userGroupsAsTextWithoutBrackets = userGroupsAsText.substring(1, userGroupsAsText.length() - 1);
		String[] userGroupsArray = userGroupsAsTextWithoutBrackets.split(",");
		List<String> userGroups = new ArrayList<>();
		for(String userGroup : userGroupsArray) {
			if(!userGroup.trim().isEmpty()) userGroups.add(userGroup);
		}
		
		return userGroups;
	}
	
	public static String transformUserGroupsToText(List<String> userGroups) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		
		for(int i = 0; i < userGroups.size(); i++) {
			String userGroup = userGroups.get(i);
			
			if(userGroup != null) {
				sb.append(userGroup.trim());
				if(i != userGroups.size() - 1) sb.append(",");
			}
		}
		sb.append("]");
		
		return sb.toString();
	}
}
