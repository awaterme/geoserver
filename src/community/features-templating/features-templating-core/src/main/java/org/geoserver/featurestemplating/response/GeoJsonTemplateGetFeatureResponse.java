/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.geojson.GeoJsonRootBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.TemplateConfiguration;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJsonWriter;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Write a valid GeoJSON output from a template */
public class GeoJsonTemplateGetFeatureResponse extends BaseTemplateGetFeatureResponse {

    protected boolean hasGeometry;

    public GeoJsonTemplateGetFeatureResponse(
            GeoServer gs, TemplateConfiguration configuration, TemplateIdentifier identifier) {
        super(gs, configuration, identifier);
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws ServiceException {

        try (GeoJsonWriter writer = getOutputWriter(output)) {
            writer.startTemplateOutput();
            iterateFeatureCollection(writer, featureCollection);
            writer.endArray();
            writeAdditionFields(writer, featureCollection, getFeature);
            writer.endTemplateOutput();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    protected GeoJsonWriter getOutputWriter(OutputStream output) throws IOException {
        return (GeoJsonWriter) helper.getOutputWriter(output);
    }

    @Override
    protected void beforeFeatureIteration(
            TemplateOutputWriter writer, RootBuilder root, FeatureTypeInfo typeInfo) {
        GeoJsonRootBuilder rb = (GeoJsonRootBuilder) root;
        GeoJsonWriter jsonWriter = (GeoJsonWriter) writer;
        String strFlatOutput =
                rb.getVendorOption(RootBuilder.VendorOption.FLAT_OUTPUT.getVendorOptionName());
        boolean flatOutput = strFlatOutput != null ? Boolean.valueOf(strFlatOutput) : false;
        jsonWriter.setFlatOutput(flatOutput);
    }

    @Override
    protected void beforeEvaluation(
            TemplateOutputWriter writer, RootBuilder root, Feature feature) {
        GeoJsonWriter geoJsonWriter = (GeoJsonWriter) writer;
        geoJsonWriter.incrementNumberReturned();
        if (!hasGeometry) {
            GeometryDescriptor descriptor = feature.getType().getGeometryDescriptor();
            if (descriptor != null) {
                Property geometry = feature.getProperty(descriptor.getName());
                hasGeometry = geometry != null;
                if (geoJsonWriter.getCrs() == null) {
                    CoordinateReferenceSystem featureCrs =
                            descriptor.getCoordinateReferenceSystem();
                    geoJsonWriter.setCrs(featureCrs);
                    geoJsonWriter.setAxisOrder(CRS.getAxisOrder(featureCrs));
                }
            }
        }
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return identifier.getOutputFormat();
    }

    protected void writeCollectionBounds(
            boolean featureBounding,
            GeoJsonWriter jsonWriter,
            List<FeatureCollection> resultsList,
            boolean hasGeom)
            throws IOException {
        // Bounding box for featurecollection
        if (hasGeom && featureBounding) {
            ReferencedEnvelope e = null;
            for (int i = 0; i < resultsList.size(); i++) {
                FeatureCollection collection = resultsList.get(i);
                if (e == null) e = collection.getBounds();
                else e.expandToInclude(collection.getBounds());
            }

            if (e != null) jsonWriter.writeBoundingBox(e);
        }
    }

    protected void writeAdditionFields(
            GeoJsonWriter writer, FeatureCollectionResponse featureCollection, Operation getFeature)
            throws IOException, FactoryException {
        BigInteger totalNumberOfFeatures = featureCollection.getTotalNumberOfFeatures();
        BigInteger featureCount =
                (totalNumberOfFeatures != null && totalNumberOfFeatures.longValue() < 0)
                        ? null
                        : totalNumberOfFeatures;

        writer.writeCollectionCounts(featureCount);
        writer.writeTimeStamp();
        String previous = featureCollection.getPrevious();
        String next = featureCollection.getNext();
        if (next != null || previous != null)
            writer.writePagingLinks(identifier.getOutputFormat(), previous, next);
        writer.writeCrs();
        writeCollectionBounds(
                getInfo().isFeatureBounding(), writer, featureCollection.getFeature(), hasGeometry);
    }
}