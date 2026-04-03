package com.tyron.completion.xml.v2;

/** The types of android resource file */
public enum XmlFileType {

  /** The AndroidManifest.xml file */
  MANIFEST,

  /** An android layout file */
  LAYOUT,

  /** An android values file (e.g., styles.xml, attrs.xml, themes.xml) */
  VALUES,

  /** An android drawable file (e.g., shape, selector) */
  DRAWABLE,

  /** An xml file but does not belong to android resource types */
  UNKNOWN
}
