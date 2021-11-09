package org.opendatakit.aggregate.odktables;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.opendatakit.aggregate.odktables.relation.DbManifestETags;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifest;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifestEntry;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.web.CallingContext;

public class FileManifestUtils {
    // TODO: Remove this hardcode when creating breaking changes to the Sync REST interface
    public static String getAppLevelManifestETag(String appId, CallingContext cc) throws ODKDatastoreException, ODKTaskLockException {
        return getAppLevelManifestETag(appId, "2", cc);
    }

    public static String getAppLevelManifestETag(String appId, String odkClientVersion, CallingContext cc) throws ODKDatastoreException, ODKTaskLockException {
        DbManifestETags.DbManifestETagEntity eTagEntity = null;
        try {
            eTagEntity = DbManifestETags.getTableIdEntry(DbManifestETags.APP_LEVEL, cc);
        } catch (ODKEntityNotFoundException e) {
            // ignore...
        }
        if(eTagEntity == null) {
            FileManifestManager manifestManager = new FileManifestManager(appId, odkClientVersion, cc);
            OdkTablesFileManifest manifest = manifestManager.getManifestForAppLevelFiles();
            String newETag = calculateUpdatedEtagFromManifest(manifest);
            eTagEntity = DbManifestETags.createNewEntity(DbManifestETags.APP_LEVEL, cc);
            eTagEntity.setManifestETag(newETag);
            eTagEntity.put(cc);
        }
        return eTagEntity.getManifestETag();
    }
    // TODO: Remove this hardcode when creating breaking changes to the Sync REST interface
    public static String getTableLevelManifestETag(String tableId, String appId, CallingContext cc)
            throws ODKDatastoreException, ODKTaskLockException {
        return getTableLevelManifestETag(tableId, appId, "2", cc);
    }

    public static String getTableLevelManifestETag(String tableId, String appId, String odkClientVersion, CallingContext cc)
            throws ODKDatastoreException, ODKTaskLockException {
        DbManifestETags.DbManifestETagEntity eTagEntity = null;
        try {
            eTagEntity = DbManifestETags.getTableIdEntry(tableId, cc);
        } catch (ODKEntityNotFoundException e) {
            // ignore...
        }
        if(eTagEntity == null) {
            FileManifestManager manifestManager = new FileManifestManager(appId, odkClientVersion, cc);
            OdkTablesFileManifest manifest = manifestManager.getManifestForTable(tableId);
            String newETag = calculateUpdatedEtagFromManifest(manifest);
            eTagEntity = DbManifestETags.createNewEntity(tableId, cc);
            eTagEntity.setManifestETag(newETag);
            eTagEntity.put(cc);
        }
        return eTagEntity.getManifestETag();
    }


    public static String calculateUpdatedEtagFromManifest(OdkTablesFileManifest manifest) {
        if(manifest == null) {
            return null;
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            for(OdkTablesFileManifestEntry entry : manifest.getFiles()) {
                try {
                    byte[] asBytes;
                    String fileMD5 = entry.md5hash;
                    if(fileMD5 != null) {
                        asBytes = fileMD5.getBytes("UTF-8");
                        md.update(asBytes);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    throw new IllegalStateException("unexpected", e);
                }

            }
            byte[] messageDigest = md.digest();

            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);
            return md5;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unexpected problem computing md5 hash", e);
        }

    }
}
