package router.dav;

import model.dav.Resource;
import model.dav.ResourceDescription;

interface ResourceMock {
	
	Resource resource();
	
	ResourceDescription resourceDescription();
}