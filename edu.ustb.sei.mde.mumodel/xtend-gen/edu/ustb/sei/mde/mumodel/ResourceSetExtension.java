package edu.ustb.sei.mde.mumodel;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.xtext.xbase.lib.Conversions;

@SuppressWarnings("all")
public class ResourceSetExtension {
  private ResourceSet resourceSet;
  
  public ResourceSetExtension() {
    this.createResourceSet();
  }
  
  protected void createResourceSet() {
    ResourceSetImpl _resourceSetImpl = new ResourceSetImpl();
    this.resourceSet = _resourceSetImpl;
    Map<String, Object> _extensionToFactoryMap = this.resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap();
    XMIResourceFactoryImpl _xMIResourceFactoryImpl = new XMIResourceFactoryImpl();
    _extensionToFactoryMap.put(
      Resource.Factory.Registry.DEFAULT_EXTENSION, _xMIResourceFactoryImpl);
  }
  
  public void registerEPackage(final EPackage... packages) {
    final Consumer<EPackage> _function = new Consumer<EPackage>() {
      public void accept(final EPackage p) {
        ResourceSetExtension.this.resourceSet.getPackageRegistry().put(p.getNsURI(), p);
      }
    };
    ((List<EPackage>)Conversions.doWrapArray(packages)).forEach(_function);
  }
  
  public Resource loadResource(final String filePath) {
    final URI uri = URI.createFileURI(filePath);
    final Resource resource = this.resourceSet.getResource(uri, true);
    return resource;
  }
  
  public Resource createResource(final String filePath) {
    final URI uri = URI.createFileURI(filePath);
    final Resource extRes = this.resourceSet.getResource(uri, false);
    if ((extRes != null)) {
      this.resourceSet.getResources().remove(extRes);
    }
    final Resource resource = this.resourceSet.createResource(uri);
    return resource;
  }
}
