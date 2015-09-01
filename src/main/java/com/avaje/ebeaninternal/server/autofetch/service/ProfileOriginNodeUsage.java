package com.avaje.ebeaninternal.server.autofetch.service;

import com.avaje.ebean.bean.NodeUsageCollector;
import com.avaje.ebean.text.PathProperties;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssoc;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.query.SplitName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Collects usages statistics for a given node in the object graph.
 */
public class ProfileOriginNodeUsage {

  private static final Logger logger = LoggerFactory.getLogger(ProfileOriginNodeUsage.class);

  private final Object monitor = new Object();

  private final String path;

  private final boolean queryTuningAddVersion;

  private int profileCount;

  private int profileUsedCount;

  private boolean modified;

  private final Set<String> aggregateUsed = new LinkedHashSet<String>();

  public ProfileOriginNodeUsage(String path, boolean queryTuningAddVersion) {
    this.path = path;
    this.queryTuningAddVersion = queryTuningAddVersion;
  }

  public void buildTunedFetch(PathProperties pathProps, BeanDescriptor<?> rootDesc) {

    synchronized (monitor) {

      BeanDescriptor<?> desc = rootDesc;
      if (path != null) {
        ElPropertyValue elGetValue = rootDesc.getElGetValue(path);
        if (elGetValue == null) {
          desc = null;
          logger.warn("AutoTune: Can't find join for path[" + path + "] for " + rootDesc.getName());

        } else {
          BeanProperty beanProperty = elGetValue.getBeanProperty();
          if (beanProperty instanceof BeanPropertyAssoc<?>) {
            desc = ((BeanPropertyAssoc<?>) beanProperty).getTargetDescriptor();
          }
        }
      }

      for (String propName : aggregateUsed) {
        BeanProperty beanProp = desc.getBeanPropertyFromPath(propName);
        if (beanProp == null) {
          logger.warn("AutoTune: Can't find property[" + propName + "] for " + desc.getName());

        } else {
          if (beanProp instanceof BeanPropertyAssoc<?>) {
            BeanPropertyAssoc<?> assocProp = (BeanPropertyAssoc<?>) beanProp;
            String targetIdProp = assocProp.getTargetIdProperty();
            String manyPath = SplitName.add(path, assocProp.getName());
            pathProps.addToPath(manyPath, targetIdProp);
          } else {
            //noinspection StatementWithEmptyBody
            if (beanProp.isLob() && !beanProp.isFetchEager()) {
              // AutoFetch will not include Lob's marked FetchLazy
              // (which is the default for Lob's so typical).
            } else {
              pathProps.addToPath(path, beanProp.getName());
            }
          }
        }
      }

      if ((modified || queryTuningAddVersion) && desc != null) {
        BeanProperty versionProp = desc.getVersionProperty();
        if (versionProp != null) {
          pathProps.addToPath(path, versionProp.getName());
        }
      }
    }
  }

  /**
   * Collect usage from a node.
   */
  public void collectUsageInfo(NodeUsageCollector profile) {

    synchronized (monitor) {

      Set<String> used = profile.getUsed();

      profileCount++;
      if (!used.isEmpty()) {
        profileUsedCount++;
        aggregateUsed.addAll(used);
      }
      if (profile.isModified()) {
        modified = true;
      }
    }
  }

  public String toString() {
    return "path[" + path + "] profileCount[" + profileCount + "] used[" + profileUsedCount + "] props" + aggregateUsed;
  }
}