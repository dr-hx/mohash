package edu.ustb.sei.mde.mumodel;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.junit.jupiter.api.Test;

@SuppressWarnings("all")
public class XTests {
  @Extension
  private ResourceSetExtension _resourceSetExtension = new ResourceSetExtension();
  
  @Test
  public void testGenID() {
    try {
      final Resource result = this._resourceSetExtension.createResource("/Users/hexiao/Projects/Java/mohash/edu.ustb.sei.mde.mumodel/model/output.xmi");
      ModelMutator.saveAsIdBasedResource(EcorePackage.eINSTANCE, ((XMIResourceImpl) result));
      result.save(System.out, null);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testModelCopy() {
    try {
      final Resource result = this._resourceSetExtension.createResource("/Users/hexiao/Projects/Java/mohash/edu.ustb.sei.mde.mumodel/model/copy.xmi");
      ModelMutator.saveAs(EcorePackage.eINSTANCE, result);
      result.save(null);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testMutation() {
    try {
      final ModelMutator mutator = new ModelMutator();
      EClass _eAnnotation = EcorePackage.eINSTANCE.getEAnnotation();
      EClass _eGenericType = EcorePackage.eINSTANCE.getEGenericType();
      EClass _eFactory = EcorePackage.eINSTANCE.getEFactory();
      EClass _eStringToStringMapEntry = EcorePackage.eINSTANCE.getEStringToStringMapEntry();
      mutator.setIgnoredClasses(new EClass[] { _eAnnotation, _eGenericType, _eFactory, _eStringToStringMapEntry });
      final Resource result = this._resourceSetExtension.createResource("/Users/hexiao/Projects/Java/mohash/edu.ustb.sei.mde.mumodel/model/output.xmi");
      mutator.mutateModel(EcorePackage.eINSTANCE, result);
      result.save(null);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
