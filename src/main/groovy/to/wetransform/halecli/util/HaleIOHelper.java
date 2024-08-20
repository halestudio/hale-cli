/*
 * Copyright (c) 2017 wetransform GmbH
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     wetransform GmbH <http://www.wetransform.to>
 */

package to.wetransform.halecli.util;

import static eu.esdihumboldt.hale.app.transform.ExecUtil.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.esdihumboldt.hale.common.core.HalePlatform;
import eu.esdihumboldt.hale.common.core.io.ExportProvider;
import eu.esdihumboldt.hale.common.core.io.HaleIO;
import eu.esdihumboldt.hale.common.core.io.ImportProvider;
import eu.esdihumboldt.hale.common.core.io.Value;
import eu.esdihumboldt.hale.common.core.io.supplier.DefaultInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.FileIOSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableInputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.LocatableOutputSupplier;
import eu.esdihumboldt.hale.common.core.io.supplier.NoStreamOutputSupplier;
import eu.esdihumboldt.hale.common.instance.io.InstanceReader;
import eu.esdihumboldt.util.Pair;

/**
 * General HaleIO helper functions
 *
 * @author Simon Templer
 */
public class HaleIOHelper {

  private static final Logger log = LoggerFactory.getLogger(HaleIOHelper.class);

  public static <T extends ExportProvider> T prepareWriter(String providerId,
      Class<T> providerClass, Map<String, String> settings, URI targetLoc) {
    // create I/O provider
    // use specified provider
    T writer = HaleIO.createIOProvider(providerClass, null, providerId);
    if (writer == null) {
      fail("Could not find export provider with ID " + providerId);
    }

    if (targetLoc != null) {
      LocatableOutputSupplier<? extends OutputStream> target = null;

      try {
        target = new FileIOSupplier(new File(targetLoc));
      } catch (Exception e) {
        // ignore
      }

      if (target == null) {
        // fall back to no stream output supplier
        target = new NoStreamOutputSupplier(targetLoc);
      }

      writer.setTarget(target);
    }

    // apply custom settings
    settings.forEach((setting, value) -> {
      writer.setParameter(setting, Value.simple(value));
    });

    return writer;
  }

  public static <T extends ImportProvider> Pair<T, String> prepareReader(URI loc,
      Class<T> providerClass, Map<String, String> settings, String customProvider) {
    LocatableInputSupplier<? extends InputStream> sourceIn = new DefaultInputSupplier(loc);

    // create I/O provider
    T reader = null;
    String providerId = null;
    if (customProvider != null) {
      // use specified provider
      reader = HaleIO.createIOProvider(providerClass, null, customProvider);
      if (reader == null) {
        fail("Could not find import provider with ID " + customProvider);
      }
      providerId = customProvider;
    }
    if (reader == null) {
      // find applicable reader
      Pair<T, String> providerInfo = HaleIO.findIOProviderAndId(providerClass, sourceIn,
          loc.getPath());
      if (providerInfo != null) {
        reader = providerInfo.getFirst();
        providerId = providerInfo.getSecond();
      }
    }
    if (reader == null) {
      throw fail("Could not determine import provider to use for source");
    }

    final T finalReader = reader;

    // apply custom settings
    settings.forEach((setting, value) -> {
      finalReader.setParameter(setting, Value.simple(value));
    });

    reader.setSource(sourceIn);

    return new Pair<>(reader, providerId);
  }

  /**
   * Guess the schema location for a given data location.
   *
   * XXX improve and move to hale codebase?
   *
   * @param dataLoc the location of the data
   * @return the location of the schema if any could be determined, otherwise
   *         <code>null</code>
   */
  @Nullable
  public static URI guessSchema(URI dataLoc) {
    if (dataLoc == null) {
      return null;
    }

    DefaultInputSupplier input = new DefaultInputSupplier(dataLoc);

    IContentTypeManager ctm = HalePlatform.getContentTypeManager();

    IContentType xml = ctm.getContentType("org.eclipse.core.runtime.xml");
    IContentType xmlGz = ctm.getContentType("eu.esdihumboldt.hale.io.xml.gzip");

    IContentType contentType = HaleIO.findContentType(InstanceReader.class, input,
        dataLoc.getPath());

    URI result = null;

    if (contentType.isKindOf(xml) || contentType.isKindOf(xmlGz)) {
      // XML -> try to determine schema via schema location
      try {
        result = getXmlSchemaLocation(input);
      } catch (Exception e) {
        log.error("Could not guess schema location from XML document", e);
      }
    }

    // TODO support for other types, especially those where the schema is extracted
    // from the data

    return result;
  }

  /**
   * Determine the XML Schema location for a given XML document.
   *
   * @param input the input supplier of the XML document
   * @return the location of the schema if any could be determined, otherwise
   *         <code>null</code>
   * @throws IOException
   * @throws XMLStreamException
   */
  private static URI getXmlSchemaLocation(DefaultInputSupplier input)
      throws IOException, XMLStreamException {
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    try (InputStream in = input.getInput()) {
      XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(in);
      boolean done = false;
      while (xmlEventReader.hasNext() && !done) {
        XMLEvent xmlEvent = xmlEventReader.nextEvent();
        if (xmlEvent.isStartElement()) {
          StartElement startElement = xmlEvent.asStartElement();

          Attribute att = startElement.getAttributeByName(
              new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "schemaLocation"));
          if (att != null) {
            String value = att.getValue();

            // XXX may hold multiple schema locations, right now we only can only handle one
            // XXX using the first one for now
            String[] parts = value.split("\\s+");
            if (parts != null && parts.length >= 2) {
              return URI.create(parts[1]);
            }
          }

          done = true;
        }
      }
    }

    return null;
  }

}
