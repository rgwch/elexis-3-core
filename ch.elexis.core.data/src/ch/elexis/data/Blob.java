/*******************************************************************************
 * Copyright (c) 2017, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.rgw.io.FileTool;
import ch.rgw.tools.ExHandler;
import io.vertx.core.json.JsonObject;

/**
 * Extracted some utility methods for BLOB-fields from PersistentObject for
 * easier unit testing.
 * 
 * @author gerry
 *
 */
public class Blob {
	static Logger log = LoggerFactory.getLogger("Extinfo");

	/**
	 * fold a byte array as stored by
	 * {@link PersistentObject#flatten(Hashtable)} or
	 * {@link Blob#toCompressedJson(Hashtable)}
	 * 
	 * @param flat
	 * @return
	 * @since 3.1
	 */
	public static Object foldObject(final byte[] flat) {
		if (flat.length == 0) {
			return null;
		}
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(flat))) {
			ZipEntry entry = null;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.getName().equals("hash")) {
					try (ObjectInputStream ois = new ObjectInputStream(zis)) {
						return ois.readObject();
					}
				} else if (entry.getName().equals("json")) {

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					FileTool.copyStreams(zis, baos);
					String stringified = new String(baos.toByteArray(), "utf-8");
					zis.closeEntry();
					zis.close();
					JsonObject jo = new JsonObject(stringified);
					Hashtable<Object, Object> ret = new Hashtable<Object, Object>();
					for (Entry<String, Object> e : jo) {
						ret.put(e.getKey(), e.getValue());
					}
					return ret;
				} else {
					log.error("foldObject: unknown format in zipped field");
					return null;
				}

			}
			zis.close();
			return null;
		} catch (Exception ex) {
			log.error("Error folding object", ex);
			return null;
		}
	}

	/**
	 * 
	 * @param object
	 * @return
	 * @since 3.1
	 * ungrad: Store as hashtable and as json
	 */
	public static byte[] flattenObject(final Object object) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(baos);
			zos.putNextEntry(new ZipEntry("hash"));
			ObjectOutputStream oos = new ObjectOutputStream(zos);
			oos.writeObject(object);
			if (object instanceof Hashtable) {
				zos.putNextEntry(new ZipEntry("json"));
				JsonObject jo = new JsonObject((Hashtable) object);
				String encoded = jo.encodePrettily();
				byte[] bytes = encoded.getBytes("utf-8");
				zos.write(bytes);
			}

			zos.close();
			baos.close();
			return baos.toByteArray();
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return null;
		}
	}

	/**
	 * Convert a Hashtable into a compressed Json Object. The resulting array is
	 * a standards conformant zipped string.
	 * 
	 * @param hash
	 *            the Hashtabe to store
	 * @return a byte array which is a compressed stringified JsonObject
	 */
	@SuppressWarnings("unchecked")
	public static byte[] toCompressedJson(final Hashtable hash) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(baos);
			zos.putNextEntry(new ZipEntry("json"));
			JsonObject jo = new JsonObject(hash);
			String encoded = jo.encodePrettily();
			byte[] bytes = encoded.getBytes("utf-8");
			zos.write(bytes);
			zos.close();
			baos.close();
			return baos.toByteArray();
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return null;
		}

	}

}
