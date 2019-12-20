package nl.idgis.publisher.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
		Collections.sort(userGroups);
		
		LinkedHashSet<String> userGroupsDistinct = new LinkedHashSet<String>(userGroups);
		
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		
		for(Iterator<String> iterator = userGroupsDistinct.iterator(); iterator.hasNext();) {
			String userGroup = iterator.next();
			
			if(userGroup != null) {
				sb.append(userGroup.trim());
				if(iterator.hasNext()) sb.append(",");
			}
		}
		sb.append("]");
		
		return sb.toString();
	}
}
