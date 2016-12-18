package ubl.amsl.technology.counter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.graph.GraphFactory;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
//import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.xml.sax.SAXException;

import ubl.amsl.technology.counter.model.version4.Contact;
import ubl.amsl.technology.counter.model.version4.Report.Customer;
import ubl.amsl.technology.counter.model.version4.DateRange;
import ubl.amsl.technology.counter.model.version4.Identifier;
import ubl.amsl.technology.counter.model.version4.Metric;
import ubl.amsl.technology.counter.model.version4.ObjectFactory;
import ubl.amsl.technology.counter.model.version4.PerformanceCounter;
import ubl.amsl.technology.counter.model.version4.Report;
import ubl.amsl.technology.counter.model.version4.ReportItem;
import ubl.amsl.technology.counter.model.version4.Reports;
import ubl.amsl.technology.counter.model.version4.Vendor;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

@SuppressWarnings("unused")
public class SushiImporter {

	private static final String type = "rdf:type";

	public static void main(String[] args) {
		getCounterGraph("");
		// Source xmlFile = null;
		// try {
		// URL schemaFile = new
		// URL("http://www.niso.org/schemas/sushi/counter4_0.xsd");
		// // xmlFile = new StreamSource(new
		// // File("counter-files/ACS_counter_4_2013.xml"));
		// // xmlFile = new StreamSource(new
		// // File("counter-files/ACS_counter_4_2013.xml"));
		// xmlFile = new StreamSource(new
		// File("counter-files/Ovid_counter_4_2013.xml"));
		// SchemaFactory schemaFactory =
		// SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		// Schema schema = schemaFactory.newSchema(schemaFile);
		// Validator validator = schema.newValidator();
		//
		// validator.validate(xmlFile);
		// System.out.println(xmlFile.getSystemId() + " is valid");
		//
		// JAXBContext jaxbContext = JAXBContext.newInstance(Reports.class);
		// Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		// // Object reportsObject = unmarshaller.unmarshal(new
		// // File("counter-files/Ovid_counter_4_2013.xml"));
		// // Reports reports = ((JAXBElement<Reports>)
		// // reportsObject).getValue();
		// Reports reports = (Reports) unmarshaller.unmarshal(new
		// File("counter-files/Ovid_counter_4_2013.xml"));
		// String test = "";
		// } catch (SAXException e) {
		// System.out.println(xmlFile.getSystemId() + " is NOT valid");
		// e.printStackTrace();
		// System.out.println("Reason: " + e.getLocalizedMessage());
		// } catch (MalformedURLException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (JAXBException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	public static void process() {
		for (String sushiSettingURI : getSushiSettingURIs()) {
			// TODO annotate sushiSettings with errors
			File counterFile = FileLoader.getLatestCounterFile(sushiSettingURI);
			if (validateCounterFile(counterFile)) {
				Reports reports = unmarshallCounterFile(counterFile);
				for (Report report : reports.getReport()) {
					processReports(report, counterFile, sushiSettingURI);
				}
			}
		}
	}

	public static void processActual(File counterFile, String organisation, String provider) {
		String sushiSettingURI = getSushiSettingUriForFolder("/" + organisation + "/" + provider);
		// TODO annotate sushiSettings with errors
		if (validateCounterFile(counterFile)) {
			Reports reports = unmarshallCounterFile(counterFile);
			for (Report report : reports.getReport()) {
				processReports(report, counterFile, sushiSettingURI);
			}
		}
	}

	public static void undoImport(String filehash, String organization, String vendor) {
		String sushiSettingURI = getSushiSettingUriForFolder("/" + organization + "/" + vendor);
		File logfile = FileLoader.getCounterImportLogFile(sushiSettingURI, filehash);
		List<String> triples = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(logfile))) {
			String line;
			while ((line = br.readLine()) != null) {
				triples.add(line);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// prepare prefixes
		List<String> prefixes = new ArrayList<>();
		prefixes.add("prefix count: <http://vocab.ub.uni-leipzig.de/counter/>");
		prefixes.add("prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>");
		prefixes.add("prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
		prefixes.add("prefix dc: <http://purl.org/dc/elements/1.1/>");
		prefixes.add("prefix skos: <http://www.w3.org/2004/02/skos/core#>");
		prefixes.add("prefix amsl: <http://vocab.ub.uni-leipzig.de/amsl/>");
		prefixes.add("prefix vcard: <http://www.w3.org/2006/vcard/ns#>");
		prefixes.add("prefix foaf: <http://xmlns.com/foaf/0.1/>");
		// get customer graph
		String graph = getCounterGraph("http://lobid.org/organisations/" + organization);
		VirtuosoConnector.getInstance().deleteTriple(triples, graph, prefixes);
	}

	/**
	 * This method maps the counter data to triples and writes them to the
	 * database. Additionally log-files are generated with triples that can be
	 * deleted without side effects and errors that may have occurred.
	 * 
	 * Internal: For a report we first need to find out information about the
	 * customer. According to the Customer a base graph is chosen where all
	 * triples will be written to, except meta data. Meta data triples like
	 * customer or vendor information, are found in
	 * <http://amsl.technology/config/counter/> an will be "updated" - differing
	 * information will be written but needs to be validated by hand against
	 * older information (which will NOT be deleted)
	 * 
	 * Error handling: NOT YET IMPLEMENTED! Error handling is done via
	 * Exceptions. At some points like opening files or folders or looking up
	 * meta data (such as vendor or customer information) errors lead to the
	 * fact that the import needs to be stopped. Exceptions are collected or may
	 * be refined thru their way upwards the call stack but finally are gathered
	 * in the public method process() where they are written to a log file
	 * and/or are refined again to user understandable texts with instructions
	 * what to do in case the processing was triggered by a human directly. In
	 * case that an error occurs while processing one report this will not
	 * effect the processing of the others contained in an Reports-Object.
	 * 
	 * @param reports
	 * @param file
	 * @param sushiSettingURI
	 */
	private static void processReports(Report report, File file, String sushiSettingURI) {

		// TODO change order of statements. We first need to find out about the
		// correct customer in order to select the valid base graph and thus to
		// build correct URIs
		String baseGraph = "";
		List<String> triplesOfOneReport = new ArrayList<>();
		List<String> metaDataTriplesOfOneReport = new ArrayList<>();

		// create ReportItems
		for (Customer customer : report.getCustomer()) {

			String customerId = getCustomerURI(customer.getID(), sushiSettingURI);
			baseGraph = getCounterGraph(customerId);
			// baseGraph = "http://localhost/OntoWiki-link/CounterTestGraph/";

			// create report resource
			String reportURI = makeUniqueUri(baseGraph);
			triplesOfOneReport.add(makeTriple(reportURI, type, "count:Report"));
			// create document resource
			String documentURI = makeUniqueUri(baseGraph);
			triplesOfOneReport.add(makeTriple(documentURI, type, "foaf:Document"));
			// link document and report
			triplesOfOneReport.add(makeTriple(reportURI, "amsl:file", documentURI));
			// add creation time to report
			String reportCreatedOn = report.getCreated().toString();
			triplesOfOneReport.add(makeTriple(reportURI, "count:wasCreatedOn",
					makeLiteral(report.getCreated().toString(), "xsd:dateTime")));
			// add report id to report
			String reportID = report.getID();
			triplesOfOneReport.add(makeTriple(reportURI, "count:hasReportID", makeLiteral(report.getID())));
			// add report version to report
			String reportVersion = report.getVersion();
			triplesOfOneReport.add(makeTriple(reportURI, "count:hasReportVersion", makeLiteral(reportVersion)));
			// add report title to report
			String reportName = report.getName();
			// TODO check if this name exists in vocabulary. Yes: add URI; No:
			// stop report import and perform error handling

			// create Vendor
			Vendor vendor = report.getVendor();
			// get the vendor URI or report failure on null
			String vendorURI = getVendorURI(vendor.getID(), sushiSettingURI);
			// TODO report failure if vendorURI == null
			triplesOfOneReport.add(makeTriple(vendorURI, type, "count:Vendor"));
			// link vendor and report
			triplesOfOneReport.add(makeTriple(vendorURI, "count:creates", reportURI));
			// add label to report
			triplesOfOneReport.add(makeTriple(reportURI, "rdfs:label",
					makeLiteral(reportCreatedOn + " " + reportName + " " + vendor.getName())));
			// add label to document
			triplesOfOneReport.add(makeTriple(documentURI, "rdfs:label",
					makeLiteral("XML " + reportCreatedOn + " " + reportName + " " + vendor.getName())));
			// add alternative label to vendor
			triplesOfOneReport.add(makeTriple(vendorURI, "skos:altLabel", makeLiteral(vendor.getName())));
			// add id to vendor
			triplesOfOneReport.add(makeTriple(vendorURI, "count:hasOrganizationID", makeLiteral(vendor.getID())));
			// add email to vendor
			for (Contact contact : report.getVendor().getContact()) {
				triplesOfOneReport.add(makeTriple(vendorURI, "vcard:hasEmail", makeLiteral(contact.getEMail())));
			}
			// add web-site URL
			triplesOfOneReport.add(makeTriple(vendorURI, "vcard:hasURL", makeLiteral(vendor.getWebSiteUrl())));

			triplesOfOneReport.add(makeTriple(customerId, "count:receives", reportURI));
			for (ReportItem reportItem : customer.getReportItems()) {
				// create ReportItemURI
				String reportItemURI = "http://TEST_REPORTITEM_URI";
				triplesOfOneReport.add(makeTriple(reportItemURI, type, "count:ReportItem"));
				// link report item and report
				triplesOfOneReport.add(makeTriple(reportItemURI, "count:isContainedIn", reportURI));
				for (Identifier identifier : reportItem.getItemIdentifier()) {
					String value = identifier.getValue();

					switch (identifier.getType()) {
					case DOI:
						// TODO check if value has valid format
						triplesOfOneReport
								.add(makeTriple(reportItemURI, "amsl:doi", makeLiteral("http://doi.org/" + value)));
						break;
					case ONLINE_ISBN:
						// TODO check if value has valid format
						triplesOfOneReport
								.add(makeTriple(reportItemURI, "amsl:eisbn", makeLiteral("urn:ISBN:" + value)));
						break;
					case EISSN:
						// TODO check if value has valid format
						triplesOfOneReport
								.add(makeTriple(reportItemURI, "amsl:eissn", makeLiteral("urn:ISSN:" + value)));
						break;
					case ONLINE_ISSN:
						// TODO check if value has valid format
						triplesOfOneReport
								.add(makeTriple(reportItemURI, "amsl:eissn", makeLiteral("urn:ISSN:" + value)));
						break;
					case ISBN:
						// TODO check if value has valid format
						triplesOfOneReport
								.add(makeTriple(reportItemURI, "amsl:pisbn", makeLiteral("urn:ISBN:" + value)));
						break;
					case PRINT_ISBN:
						// TODO check if value has valid format
						triplesOfOneReport
								.add(makeTriple(reportItemURI, "amsl:pisbn", makeLiteral("urn:ISBN:" + value)));
						break;
					case ISSN:
						// TODO check if value has valid format
						triplesOfOneReport
								.add(makeTriple(reportItemURI, "amsl:pissn", makeLiteral("urn:ISSN:" + value)));
						break;
					case PRINT_ISSN:
						// TODO check if value has valid format
						triplesOfOneReport
								.add(makeTriple(reportItemURI, "amsl:pissn", makeLiteral("urn:ISSN:" + value)));
						break;
					case PROPRIETARY:
						// TODO check if value has valid format??? Inform
						// responsible person???
						triplesOfOneReport.add(makeTriple(reportItemURI, "amsl:proprietaryID", makeLiteral(value)));
						break;
					}
				}
				// reports >> report >> customer >> reportItem >> platform
				// get platformURI stored with SushiData according to
				// platformId
				String platformURI = getPlatformURI(reportItem.getItemPlatform(), sushiSettingURI);
				triplesOfOneReport.add(makeTriple(platformURI, type, "count:Platform"));
				triplesOfOneReport
						.add(makeTriple(platformURI, "skos:altLabel ", makeLiteral(reportItem.getItemPlatform())));
				// link report item and platform
				triplesOfOneReport.add(makeTriple(reportItemURI, "count:isAccessibleVia", platformURI));
				// reports >> report >> customer >> reportItem >> publisher
				String publisherURI = getPublisherURI(reportItem.getItemPublisher(), sushiSettingURI);
				triplesOfOneReport.add(makeTriple(publisherURI, type, "count:Publisher"));
				triplesOfOneReport
						.add(makeTriple(publisherURI, "skos:altLabel ", makeLiteral(reportItem.getItemPublisher())));
				// link report item and platform
				triplesOfOneReport.add(makeTriple(reportItemURI, "dc:publisher", publisherURI));
				// reports >> report >> customer >> reportItem >> item name
				triplesOfOneReport.add(makeTriple(reportItemURI, "rdfs:label",
						makeLiteral(reportItem.getItemName() + " [ReportItem]")));
				triplesOfOneReport
						.add(makeTriple(reportItemURI, "count:hasItemName", makeLiteral(reportItem.getItemName())));
				// reports >> report >> customer >> reportItem >> item data
				// type
				triplesOfOneReport.add(
						makeTriple(reportItemURI, "count:hasItemDatatype", "count:" + reportItem.getItemDataType()));

				// reports >> report >> customer >> reportItem >>
				// itemPerformance
				for (Metric itemPerformance : reportItem.getItemPerformance()) {
					List<String> metricTriple = createMetricTripel(itemPerformance);
					String dtRg = "";
					if (metricTriple == null) {
						// TODO return with error
					} else {
						dtRg = metricTriple.get(0);
						metricTriple.remove(0);
						triplesOfOneReport.addAll(metricTriple);
					}

					List<String> periodTriple = createPeriodTripel(itemPerformance.getPeriod());
					String period_dtRg = "";
					if (periodTriple == null) {
						// TODO return with error
					} else {
						period_dtRg = periodTriple.get(0);
						periodTriple.remove(0);
						triplesOfOneReport.addAll(periodTriple);
					}

					// create countingInstances
					for (PerformanceCounter instance : itemPerformance.getInstance()) {
						// create Counting instance
						String countingInstanceURI = "<http://ubl.amsl.technology/statistics/Cntl_"
								+ UUID.randomUUID().toString() + ">";
						triplesOfOneReport.add(makeTriple(countingInstanceURI, "rdf:type", "count:CountingInstance"));
						// add label
						triplesOfOneReport.add(makeTriple(countingInstanceURI, "rdfs:label",
								makeLiteral("Count " + itemPerformance.getCategory().toString() + " "
										+ instance.getMetricType().toString() + " "
										+ itemPerformance.getPeriod().getBegin().toString() + "-"
										+ itemPerformance.getPeriod().getEnd().toString())));
						// add metric
						triplesOfOneReport.add(makeTriple(countingInstanceURI, "count:hasMetricType",
								"count:" + instance.getMetricType().toString()));
						// add count value
						triplesOfOneReport.add(makeTriple(countingInstanceURI, "count:hasCount",
								makeLiteral(instance.getCount().toString(), "xsd:decimal")));
						// add category
						triplesOfOneReport.add(makeTriple(countingInstanceURI, "count:hasCategory",
								"count:" + itemPerformance.getCategory().toString()));
						// link to period
						triplesOfOneReport.add(makeTriple(countingInstanceURI, "count:measuredForPeriod", period_dtRg));
						// link to date range
						triplesOfOneReport.add(makeTriple(countingInstanceURI, "count:considersPubYear", dtRg));
						// link to report item
						triplesOfOneReport.add(makeTriple(reportItemURI, "count:hasPerformance", countingInstanceURI));
					}
				}
			}

		}
		// prepare prefixes
		List<String> prefixes = new ArrayList<>();
		prefixes.add("prefix count: <http://vocab.ub.uni-leipzig.de/counter/>");
		prefixes.add("prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>");
		prefixes.add("prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
		prefixes.add("prefix dc: <http://purl.org/dc/elements/1.1/>");
		prefixes.add("prefix skos: <http://www.w3.org/2004/02/skos/core#>");
		prefixes.add("prefix amsl: <http://vocab.ub.uni-leipzig.de/amsl/>");
		prefixes.add("prefix vcard: <http://www.w3.org/2006/vcard/ns#>");
		prefixes.add("prefix foaf: <http://xmlns.com/foaf/0.1/>");
		VirtuosoConnector.getInstance().writeTriple(triplesOfOneReport, baseGraph, prefixes);
		// VirtuosoConnector.getInstance().writeTriple(triplesOfOneReport,
		// baseGraph);
		writeLog(triplesOfOneReport, file);

	}

	private static String addAngleBrackets(String uri) {
		return "<" + uri + ">";
	}

	private static String makeLiteral(String value, String type) {
		String result = makeLiteral(value);
		if (type != null) {
			result = result + "^^" + type;
		}
		return result;
	}

	private static String makeLiteral(String value) {
		return '"' + value + '"';
	}

	private static String makeUniqueUri(String baseURI) {
		return baseURI + "/" + UUID.randomUUID();
	}

	private static String makeTriple(String subject, String predicate, String object) {
		if (subject.startsWith("http://")) {
			subject = addAngleBrackets(subject);
		}
		if (predicate.startsWith("http://")) {
			predicate = addAngleBrackets(predicate);
		}
		if (object.startsWith("http://")) {
			object = addAngleBrackets(object);
		}
		return subject + " " + predicate + " " + object + " .";
	}

	private static String getVendorURI(String vendorID, String sushiSettingURI) {
		VirtuosoConnector db = VirtuosoConnector.getInstance();
		String query = "select distinct ?vendorUriMapping from <http://amsl.technology/config/counter/> where {<"
				+ getCounterSupplier(sushiSettingURI)
				+ "> <http://vocab.ub.uni-leipzig.de/terms/mappingVendorID> ?vendorUriMapping}";
		ResultSet results = db.query(query);
		while (results.hasNext()) {
			QuerySolution rs = results.nextSolution();
			RDFNode o = rs.get("vendorUriMapping");
			if (o.isLiteral()) {
				String[] mapping = o.asLiteral().getValue().toString().split((Pattern.quote("|")));
				if (mapping[0].equals(vendorID)) {
					return mapping[1];
				}
			}
		}
		return null;
	}

	private static String getCounterSupplier(String sushiSettingURI) {
		VirtuosoConnector db = VirtuosoConnector.getInstance();
		String query = "select distinct ?counterSupplier from <http://amsl.technology/config/counter/> where { ?counterSupplier <http://vocab.ub.uni-leipzig.de/terms/sushiSettings> <"
				+ sushiSettingURI + "> }";
		ResultSet results = db.query(query);
		while (results.hasNext()) {
			QuerySolution rs = results.nextSolution();
			RDFNode o = rs.get("counterSupplier");
			if (o.isResource()) {
				return o.asResource().getURI();
			}
		}
		return null;
	}

	private static String getPlatformURI(String platformID, String sushiSettingURI) {
		VirtuosoConnector db = VirtuosoConnector.getInstance();
		String query = "select distinct ?platformUriMapping from <http://amsl.technology/config/counter/> where {<"
				+ getCounterSupplier(sushiSettingURI)
				+ "> <http://vocab.ub.uni-leipzig.de/terms/mappingItemPlatform> ?platformUriMapping}";
		ResultSet results = db.query(query);
		while (results.hasNext()) {
			QuerySolution rs = results.nextSolution();
			RDFNode o = rs.get("platformUriMapping");
			if (o.isLiteral()) {
				String[] mapping = o.asLiteral().getValue().toString().split(Pattern.quote("|"));
				if (mapping[0].equals(platformID)) {
					return mapping[1];
				}
			}
		}
		return null;
	}

	private static String getPublisherURI(String publisherID, String sushiSettingURI) {
		VirtuosoConnector db = VirtuosoConnector.getInstance();
		String query = "select distinct ?publisherUriMapping from <http://amsl.technology/config/counter/> where {<"
				+ getCounterSupplier(sushiSettingURI)
				+ "> <http://vocab.ub.uni-leipzig.de/terms/mappingItemPublisher> ?publisherUriMapping}";
		ResultSet results = db.query(query);
		while (results.hasNext()) {
			QuerySolution rs = results.nextSolution();
			RDFNode o = rs.get("publisherUriMapping");
			if (o.isLiteral()) {
				String[] mapping = o.asLiteral().getValue().toString().split(Pattern.quote("|"));
				if (mapping[0].equals(publisherID)) {
					return mapping[1];
				}
			}
		}
		return null;
	}

	private static String getCustomerURI(String customerID, String sushiSettingURI) {
		VirtuosoConnector db = VirtuosoConnector.getInstance();
		String query = "select distinct ?customerUriMapping from <http://amsl.technology/config/counter/> where {<"
				+ getCounterSupplier(sushiSettingURI)
				+ "> <http://vocab.ub.uni-leipzig.de/terms/mappingCustomerID> ?customerUriMapping}";
		ResultSet results = db.query(query);
		while (results.hasNext()) {
			QuerySolution rs = results.nextSolution();
			RDFNode o = rs.get("customerUriMapping");
			if (o.isLiteral()) {
				String[] mapping = o.asLiteral().getValue().toString().split(Pattern.quote("|"));
				if (mapping[0].equals(customerID)) {
					return mapping[1];
				}
			}
		}
		return null;
	}

	private static List<String> getSushiSettingURIs() {
		List<String> result = new ArrayList<>();
		VirtuosoConnector db = VirtuosoConnector.getInstance();
		String query = "select distinct ?sushiSetting from <http://amsl.technology/config/counter/> where {?sushiSetting <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.ub.uni-leipzig.de/terms/SushiSetting>}";
		ResultSet results = db.query(query);
		while (results.hasNext()) {
			QuerySolution rs = results.nextSolution();
			RDFNode o = rs.get("sushiSetting");
			if (o.isResource()) {
				result.add(o.asResource().getURI());
			}
		}
		return result;
	}

	private static String getSushiSettingUriForFolder(String folder) {

		VirtuosoConnector db = VirtuosoConnector.getInstance();
		String query = "select distinct ?sushiSetting from <http://amsl.technology/config/counter/> where {?sushiSetting <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.ub.uni-leipzig.de/terms/SushiSetting>. ?sushiSetting <http://vocab.ub.uni-leipzig.de/terms/linkToStatsFolder> \""
				+ folder + "/\"}";
		ResultSet results = db.query(query);
		while (results.hasNext()) {
			QuerySolution rs = results.nextSolution();
			RDFNode o = rs.get("sushiSetting");
			if (o.isResource()) {
				return o.asResource().getURI();
			}
		}
		// Sorry for this dirty code. the follwing construct tries to fiend the folder without ending "/"
		query = "select distinct ?sushiSetting from <http://amsl.technology/config/counter/> where {?sushiSetting <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.ub.uni-leipzig.de/terms/SushiSetting>. ?sushiSetting <http://vocab.ub.uni-leipzig.de/terms/linkToStatsFolder> \""
				+ folder + "\"}";
		results = db.query(query);
		while (results.hasNext()) {
			QuerySolution rs = results.nextSolution();
			RDFNode o = rs.get("sushiSetting");
			if (o.isResource()) {
				return o.asResource().getURI();
			}
		}
		
		return null;
	}

	private static String getCounterGraph(String customerId) {

		String query = "SELECT Distinct ?graph WHERE { GRAPH ?graph { ?graph <http://vocab.ub.uni-leipzig.de/terms/validForOrganization> <"
				+ customerId + "> } . }";
		VirtuosoConnector db = VirtuosoConnector.getInstance();
		ResultSet results = db.query(query);
		while (results.hasNext()) {
			QuerySolution rs = results.nextSolution();
			RDFNode o = rs.get("graph");
			if (o.isResource()) {
				return o.asResource().getURI();
			}
		}
		return null;
	}

	private static boolean validateCounterFile(File counterfile) {
		try {

			File schemaFile = new File(FileLoader.getRootFolder() + "/schemas/counter/counter4_0.xsd");
			Source xmlFile = new StreamSource(counterfile);
			SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			schemaFactory.setResourceResolver(new ResourceResolver());
			Schema schema = schemaFactory.newSchema(new StreamSource(schemaFile));
			Validator validator = schema.newValidator();
			validator.validate(xmlFile);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			System.out.println("Reason: " + e.getLocalizedMessage());
			return false;
		}
		return true;
	}

	private static Reports unmarshallCounterFile(File counterfile) {
		Reports reports = null;
		try {
			// validate XML
			URL schemaFile = new URL("http://www.niso.org/schemas/sushi/counter4_0.xsd");
			Source xmlFile = new StreamSource(counterfile);
			SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			Schema schema = schemaFactory.newSchema(schemaFile);
			Validator validator = schema.newValidator();
			validator.validate(xmlFile);

			// convert XML to Java
			JAXBContext jaxbContext = JAXBContext.newInstance(Reports.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			reports = (Reports) unmarshaller.unmarshal(counterfile);
		} catch (SAXException e) {
			e.printStackTrace();
			System.out.println("Reason: " + e.getLocalizedMessage());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return reports;
	}

	private static List<String> createMetricTripel(Metric itemPerformance) {
		List<String> metricTriples = new ArrayList<>();
		String years = "";
		String label = "";
		// only pubYr available
		if (itemPerformance.getPubYr() != null && itemPerformance.getPubYrFrom() == null
				&& itemPerformance.getPubYrTo() == null) {
			years = new Integer(itemPerformance.getPubYr().getYear()).toString();
			label = new Integer(itemPerformance.getPubYr().getYear()).toString();
			String dtRg = "http://ubl.amsl.technology/statistics/DtRg_" + years;
			metricTriples.add(dtRg);
			metricTriples.add(makeTriple(dtRg, "rdf:type", "count:DateRange"));
			metricTriples.add(makeTriple(dtRg, "rdfs:label", makeLiteral(label)));
			metricTriples.add(makeTriple(dtRg, "count:hasStartDay", makeLiteral(years + "-01-01", "xsd:date")));
			metricTriples.add(makeTriple(dtRg, "count:hasEndDay", makeLiteral(years + "-12-31", "xsd:date")));
		}
		// publication range available
		if (itemPerformance.getPubYr() == null && itemPerformance.getPubYrFrom() != null
				&& itemPerformance.getPubYrTo() != null) {
			// check if PubYrTo is equal or higher than PubYrFrom
			LocalDate from = itemPerformance.getPubYrFrom().toGregorianCalendar().toZonedDateTime().toLocalDate();
			LocalDate to = itemPerformance.getPubYrTo().toGregorianCalendar().toZonedDateTime().toLocalDate();
			if (from.isBefore(to) || from.isEqual(to)) {
				years = itemPerformance.getPubYrFrom().toString() + "-" + itemPerformance.getPubYrTo().toString();
				label = new String(
						itemPerformance.getPubYrFrom().getYear() + "-" + itemPerformance.getPubYrTo().getYear());
				String dtRg = "http://ubl.amsl.technology/statistics/DtRg_" + years;
				metricTriples.add(dtRg);
				metricTriples.add(makeTriple(dtRg, "rdf:type", "count:DateRange"));
				metricTriples.add(makeTriple(dtRg, "rdfs:label", makeLiteral(label)));
				metricTriples.add(makeTriple(dtRg, "count:hasStartDay",
						makeLiteral(itemPerformance.getPubYrFrom().toString(), "xsd:date")));
				metricTriples.add(makeTriple(dtRg, "count:hasEndDay",
						makeLiteral(itemPerformance.getPubYrTo().toString(), "xsd:date")));
			} else {
				return null;
			}
		}
		// every thing available
		if (itemPerformance.getPubYr() != null && itemPerformance.getPubYrFrom() != null
				&& itemPerformance.getPubYrTo() != null) {
			// check if PubYrTo equals PubYrFrom and equals pubYr
			LocalDate from = itemPerformance.getPubYrFrom().toGregorianCalendar().toZonedDateTime().toLocalDate();
			LocalDate to = itemPerformance.getPubYrTo().toGregorianCalendar().toZonedDateTime().toLocalDate();
			LocalDate single = itemPerformance.getPubYrTo().toGregorianCalendar().toZonedDateTime().toLocalDate();
			if (from.getYear() == to.getYear() && from.getYear() == single.getYear()
					&& (from.isBefore(to) || from.isEqual(to))) {
				years = itemPerformance.getPubYr().toString();
				label = new Integer(itemPerformance.getPubYr().getYear()).toString();
				String dtRg = "http://ubl.amsl.technology/statistics/DtRg_" + years;
				metricTriples.add(dtRg);
				metricTriples.add(makeTriple(dtRg, "rdf:type", "count:DateRange"));
				metricTriples.add(makeTriple(dtRg, "rdfs:label", makeLiteral(label)));
				metricTriples.add(makeTriple(dtRg, "count:hasStartDay",
						makeLiteral(itemPerformance.getPubYrFrom().toString(), "xsd:date")));
				metricTriples.add(makeTriple(dtRg, "count:hasEndDay",
						makeLiteral(itemPerformance.getPubYrTo().toString(), "xsd:date")));
			} else {
				return null;
			}
		}
		// nothing available
		if (itemPerformance.getPubYr() == null && itemPerformance.getPubYrFrom() == null
				&& itemPerformance.getPubYrTo() == null) {
			String dtRg = "http://ubl.amsl.technology/statistics/DtRg_unknown";
			metricTriples.add(dtRg);
			metricTriples.add(makeTriple(dtRg, "rdf:type", "count:DateRange"));
			metricTriples.add(makeTriple(dtRg, "rdfs:label", makeLiteral("no publication time information specified")));
		}
		return metricTriples;
	}

	private static List<String> createPeriodTripel(DateRange period) {
		// TODO Abbrechen und Fehlermeldung falls period == null

		List<String> periodTriples = new ArrayList<>();
		// creating the measurement period if given
		String period_dtRg = "";
		// if (itemPerformance.getPeriod() != null) {
		String periodYears = period.getBegin().toString() + "-" + period.getEnd().toString();
		period_dtRg = "http://ubl.amsl.technology/statistics/DtRg_" + periodYears;
		periodTriples.add(period_dtRg);
		String period_label = period.getBegin() + "-" + period.getEnd();
		periodTriples.add(makeTriple(period_dtRg, "rdf:type", "count:DateRange"));
		periodTriples.add(makeTriple(period_dtRg, "rdfs:label", makeLiteral(period_label)));
		periodTriples.add(
				makeTriple(period_dtRg, "count:hasStartDay", makeLiteral(period.getBegin().toString(), "xsd:date")));
		periodTriples
				.add(makeTriple(period_dtRg, "count:hasEndDay", makeLiteral(period.getEnd().toString(), "xsd:date")));
		// }
		return periodTriples;
	}

	private static void writeLog(List<String> triples, File counterFile) {
		try {
			String logDirectoryPath = counterFile.getParentFile() + "/logs/";
			File logDirectory = new File(logDirectoryPath);
			if (!logDirectory.exists()) {
				logDirectory.mkdirs();
			}
			String logFilePath = logDirectoryPath + counterFile.getName() + ".log";
			File logFile = new File(logFilePath);
			logFile.createNewFile();
			FileWriter writer = new FileWriter(logFile);
			for (String str : triples) {
				writer.write(str + System.getProperty("line.separator"));
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}