package ubl.amsl.technology.counter;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/counter")
public class CounterController {
	
	@GetMapping("/supplier")
	public List<CounterSupplier> getCounterSupplier(){
		List<CounterSupplier> results = new ArrayList<>();
		ResultSet couterSupplierQueryResults = VirtuosoConnector.getInstance().query("select distinct ?counterSupplier ?label from <http://amsl.technology/config/counter/> where { ?counterSupplier <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.ub.uni-leipzig.de/terms/CounterSupplier> . ?counterSupplier <http://www.w3.org/2000/01/rdf-schema#label> ?label .}");
		while (couterSupplierQueryResults.hasNext()) {
			CounterSupplier cs = new CounterSupplier();
			QuerySolution rs = couterSupplierQueryResults.nextSolution();
			RDFNode o = rs.get("counterSupplier");
			if (o.isResource()) {
				cs.setUri(o.asResource().getURI());
			}
			o = rs.get("label");
			if (o.isLiteral()) {
				cs.setLabel(o.asLiteral().getValue().toString());
			}
			System.out.println(cs.getUri());
			ResultSet sushiSettingQueryResults = VirtuosoConnector.getInstance().query("select distinct ?sushiSetting ?folder ?label ?organisation from <http://amsl.technology/config/counter/> where { <http://amsl.technology/config/counter/countersupplier/acs> <http://vocab.ub.uni-leipzig.de/terms/sushiSettings> ?sushiSetting . ?sushiSetting <http://vocab.ub.uni-leipzig.de/terms/linkToStatsFolder> ?folder . ?sushiSetting <http://vocab.ub.uni-leipzig.de/terms/validForOrganization> ?organisation . ?sushiSetting <http://www.w3.org/2000/01/rdf-schema#label> ?label . }");
			while (sushiSettingQueryResults.hasNext()) {
				SushiSetting ss = new SushiSetting();
				QuerySolution rsSS = sushiSettingQueryResults.nextSolution();
				RDFNode oSS = rsSS.get("sushiSetting");
				if (oSS.isResource()) {
					ss.setUri(oSS.asResource().getURI());
				}
				oSS = rsSS.get("label");
				if (oSS.isLiteral()) {
					ss.setLabel(oSS.asLiteral().getValue().toString());
				}
				oSS = rsSS.get("folder");
				if (oSS.isLiteral()) {
					ss.setFolder(oSS.asLiteral().getValue().toString());
				}
				oSS = rsSS.get("organisation");
				if (oSS.isResource()) {
					ss.setOrganisation(oSS.asResource().getURI());
				}
				cs.getSushiSettings().add(ss);
			}
			results.add(cs);
		}
		return results;
	}
	
	public class CounterSupplier{
		private String uri = "";
		private String label = "";
		private List<SushiSetting> sushiSettings = new ArrayList<>();
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public List<SushiSetting> getSushiSettings() {
			return sushiSettings;
		}
		public void setSushiSettings(List<SushiSetting> sushiSettings) {
			this.sushiSettings = sushiSettings;
		}
		public String getUri() {
			return uri;
		}
		public void setUri(String uri) {
			this.uri = uri;
		}
	}
	
	public class SushiSetting{
		private String uri = "";
		private String folder = "";
		private String organisation = "";
		private String label = "";
		
		public String getFolder() {
			return folder;
		}
		public void setFolder(String folder) {
			this.folder = folder;
		}
		public String getOrganisation() {
			return organisation;
		}
		public void setOrganisation(String organisation) {
			this.organisation = organisation;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public String getUri() {
			return uri;
		}
		public void setUri(String uri) {
			this.uri = uri;
		}
	}
}
