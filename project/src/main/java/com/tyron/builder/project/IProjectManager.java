package com.tyron.builder.project;

import org.appdevforall.codeonthego.indexing.service.IndexingServiceManager;

import java.io.File;

public class IProjectManager {

  public volatile Project currentProject;
  private static IProjectManager instance;
  private final IndexingServiceManager indexingServiceManager = new IndexingServiceManager();

  public static IProjectManager getInstance() {
    if (instance == null) {
      return instance = new IProjectManager();
    }
    return instance;
  }

  public File getProjectDir() {
    if (currentProject == null) return null;
    return currentProject.getRootFile();
  }

  public String getProjectDirPath() {
    if (getProjectDir() == null) return null;
    return getProjectDir().getPath();
  }

    public IndexingServiceManager getIndexingServiceManager() {
//        if(indexingServiceManager ==null) indexingServiceManager = new IndexingServiceManager();
        return indexingServiceManager;
    }
}
