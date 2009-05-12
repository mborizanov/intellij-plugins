/*
 * Copyright (c) 2007-2009, Osmorc Development Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright notice, this list
 *       of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this
 *       list of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *     * Neither the name of 'Osmorc Development Team' nor the names of its contributors may be
 *       used to endorse or promote products derived from this software without specific
 *       prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.osmorc.frameworkintegration;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import org.junit.After;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.osmorc.TestUtil;
import org.osmorc.settings.ProjectSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Robert F. Beeger (robert@beeger.net)
 */
@SuppressWarnings({"ConstantConditions"})
public class FrameworkInstanceModuleManagerTest
{
  public FrameworkInstanceModuleManagerTest()
  {
    _fixture = TestUtil.createTestFixture();
  }

  @Before
  public void setUp() throws Exception
  {
    _fixture.setUp();
    Application application = ApplicationManager.getApplication();
    _projectSettings = new ProjectSettings();
    _testObject = new FrameworkInstanceModuleManager(ServiceManager.getService(LibraryHandler.class), _projectSettings,
        application, _fixture.getProject(),
        ModuleManager.getInstance(_fixture.getProject()));
    _project = _fixture.getProject();
    application.runWriteAction(new Runnable()
    {
      public void run()
      {
        LibraryTable.ModifiableModel modifiableModel =
            LibraryTablesRegistrar.getInstance().getLibraryTable().getModifiableModel();
        _libraryA1 = modifiableModel.createLibrary("Osmorc/an Instance/libA1_1.0.0");
        _libraryA2 = modifiableModel.createLibrary("Osmorc/an Instance/libA2_1.0.0");
        _libraryB1 = modifiableModel.createLibrary("Osmorc/another Instance/libB1_1.0.0");
        _libraryB2 = modifiableModel.createLibrary("Osmorc/another Instance/libB2_1.0.0");
        _libraryB3 = modifiableModel.createLibrary("Osmorc/another Instance/libB3_1.0.0");
        modifiableModel.commit();
      }
    });

  }

  @After
  public void tearDown() throws Exception
  {
    _fixture.tearDown();
  }

  @Test
  public void testUpdateFrameworkInstanceModule()
  {
    _projectSettings.setCreateFrameworkInstanceModule(true);
    _projectSettings.setFrameworkInstanceName("an Instance");
    _testObject.updateFrameworkInstanceModule();
    Module module = ModuleManager.getInstance(_project)
        .findModuleByName(FrameworkInstanceModuleManager.FRAMEWORK_INSTANCE_MODULE_NAME);
    ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
    OrderEntry[] orderEntries = model.getOrderEntries();
    model.dispose();
    assertContainsOnlyGivenLibraries(orderEntries, _libraryA1, _libraryA2);

    _projectSettings.setFrameworkInstanceName("another Instance");
    _testObject.updateFrameworkInstanceModule();
    ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
    orderEntries = rootModel.getOrderEntries();
    rootModel.dispose();
    assertContainsOnlyGivenLibraries(orderEntries, _libraryB1, _libraryB2, _libraryB3);

    _projectSettings.setCreateFrameworkInstanceModule(false);
    _projectSettings.setFrameworkInstanceName("an Instance");
    _testObject.updateFrameworkInstanceModule();
    module = ModuleManager.getInstance(_project)
        .findModuleByName(FrameworkInstanceModuleManager.FRAMEWORK_INSTANCE_MODULE_NAME);
    assertThat(module, nullValue());
  }

  public void assertContainsOnlyGivenLibraries(OrderEntry[] orderEntries, Library... libraries)
  {
    List<Library> libs = new ArrayList<Library>(Arrays.asList(libraries));
    int findCount = 0;

    for (OrderEntry orderEntry : orderEntries)
    {
      if (orderEntry instanceof LibraryOrderEntry)
      {
        assertTrue(libs.remove(((LibraryOrderEntry) orderEntry).getLibrary()));
        findCount++;
      }
    }

    assertThat(libraries.length, equalTo(findCount));
  }

  private IdeaProjectTestFixture _fixture;
  private FrameworkInstanceModuleManager _testObject;
  private Project _project;

  private Library _libraryA1;
  private Library _libraryA2;
  private Library _libraryB1;
  private Library _libraryB2;
  private Library _libraryB3;
  private ProjectSettings _projectSettings;
}