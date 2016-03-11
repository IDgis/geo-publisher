package router.dav;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import model.dav.Resource;
import model.dav.ResourceDescription;
import model.dav.ResourceProperties;

class SimpleWebDAVMock extends SimpleWebDAV {
	
	final List<ResourceMock> testResources;
	
	public SimpleWebDAVMock(List<ResourceMock> testResources) {
		this(Collections.emptyList(), testResources);
	}
	
	public SimpleWebDAVMock(List<SimpleWebDAV> directories, List<ResourceMock> testResources) {
		this("/", directories, testResources);
	}
	
	public SimpleWebDAVMock(String prefix, List<ResourceMock> testResources) {
		this(prefix, Collections.emptyList(), testResources);
	}
	
	public SimpleWebDAVMock(String prefix, List<SimpleWebDAV> directories, List<ResourceMock> testResources) {
		super(prefix, directories);
		
		this.testResources = testResources;
	}
	
	@Override
	public SimpleWebDAVMock withPrefix(String prefix) {
		return new SimpleWebDAVMock(prefix, testResources);
	}
	
	@Override
	public Optional<Resource> resource(String name) {
		return testResources.stream()
			.filter(testResource -> testResource.resourceDescription().name().equals(name))
			.map(testResource -> testResource.resource())
			.findFirst();
	}
	
	@Override
	public Stream<ResourceDescription> descriptions() {
		return testResources.stream()
			.map(testResource -> testResource.resourceDescription());
	}
	
	@Override
	public Optional<ResourceProperties> properties(String name) {
		return testResources.stream()
			.map(testResource -> testResource.resourceDescription())
			.filter(resourceDescription -> resourceDescription.name().equals(name))
			.map(resourceDescription -> resourceDescription.properties())
			.findFirst();
	}
}