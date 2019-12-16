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
}
