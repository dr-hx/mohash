package edu.ustb.sei.mde.mumodel

import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.common.util.URI

class ResourceSetExtension {
	var ResourceSet resourceSet
	
	new() {
		createResourceSet
	}
	
	protected def void createResourceSet() {
		resourceSet = new ResourceSetImpl
		// Register the default resource factory -- only needed for stand-alone!
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
			Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
	}
	
	def void registerEPackage(EPackage... packages) {
		packages.forEach[p|
			resourceSet.packageRegistry.put(p.nsURI, p)
		]
	}
	
	def Resource loadResource(String filePath) {
		val uri = URI.createFileURI(filePath)
		val resource = resourceSet.getResource(uri, true);
		return resource
	}
	
	def Resource createResource(String filePath) {
		val uri = URI.createFileURI(filePath)
		val extRes = resourceSet.getResource(uri, false)
		if(extRes!==null) resourceSet.resources.remove(extRes);
		val resource = resourceSet.createResource(uri);
		return resource
	}
}