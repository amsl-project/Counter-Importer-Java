package ubl.amsl.technology.counter;

import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;

import com.google.common.collect.Lists;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

public class VirtuosoConnector {

	private static VirtuosoConnector instance = null;
	private VirtGraph set = null;

	private VirtuosoConnector() {
		// TODO Load db-connection info from file
		set = new VirtGraph("jdbc:virtuoso://localhost:1111", "dba", "dba");
	}

	public static VirtuosoConnector getInstance() {
		if (instance == null) {
			instance = new VirtuosoConnector();
		}
		return instance;
	}

	public void writeTriple(List<String> tripels, String graph, List<String> prefixes) {
		List<List<String>> triplePartions = Lists.partition(tripels, 1000);
		for (List<String> triplePartition : triplePartions) {
			String query = "";
			for (String prefixStatement : prefixes) {
				query = query + prefixStatement + System.getProperty("line.separator");
			}
			query = query + "INSERT INTO GRAPH <" + graph + "> {" + System.getProperty("line.separator");
			for (String triple : triplePartition) {
				query = query + triple + System.getProperty("line.separator");
			}
			query = query + "}";

			VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, set);
			vur.exec();
		}
	}

	public void deleteTriple(List<String> tripels, String graph, List<String> prefixes) {
		List<List<String>> triplePartions = Lists.partition(tripels, 1000);
		for (List<String> triplePartition : triplePartions) {
			String query = "";
			for (String prefixStatement : prefixes) {
				query = query + prefixStatement + System.getProperty("line.separator");
			}
			query = query + "DELETE DATA { " + System.getProperty("line.separator") + "GRAPH <" + graph + "> { "
					+ System.getProperty("line.separator");
			for (String triple : triplePartition) {
				query = query + triple + System.getProperty("line.separator");
			}
			query = query + "}}";

			VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, set);
			vur.exec();
		}
	}

	public ResultSet query(String query) {
		// create query Object
		Query sparql = QueryFactory.create(query);
		// create execution object
		VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, set);
		// query data store
		return vqe.execSelect();
	}
}
