package edu.ustb.sei.mde.mumodel

import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.common.util.URI
import org.eclipse.uml2.uml.resource.UMLResource
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.uml2.uml.UMLPackage
import org.eclipse.uml2.uml.resource.XMI2UMLResource

class ResourceSetExtension {
	var ResourceSet resourceSet
	var ResourceSet umlResourceSet
	
	new() {
		createResourceSet
	}
	
	protected def void createResourceSet() {
		resourceSet = new ResourceSetImpl
		// Register the default resource factory -- only needed for stand-alone!
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(
			Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
			
		umlResourceSet = new ResourceSetImpl
		umlResourceSet.packageRegistry.put(UMLPackage.eINSTANCE.nsURI, UMLPackage.eINSTANCE)
		umlResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(XMI2UMLResource.FILE_EXTENSION, XMI2UMLResource.Factory.INSTANCE) 
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
	
	def Resource loadUMLResource(String filePath) {
		val uri = URI.createFileURI(filePath)
		val resource = umlResourceSet.getResource(uri, true);
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