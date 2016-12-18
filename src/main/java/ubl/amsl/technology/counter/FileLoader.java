package ubl.amsl.technology.counter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

public class FileLoader {

	public static File getRootFolder() {
		return new File(System.getProperty("user.dir"));
	}

	public static File getLatestCounterFile(String sushiSettingURI) {
		String folder = getCounterDataFolder(sushiSettingURI);
//		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();

			// get the latest (last modified!) file
			// inspired by:
			// http://stackoverflow.com/questions/285955/java-get-the-newest-file-in-a-directory
//			Resource resource = appContext.getResource("classpath:counterfiles" + folder);
			File dir = new File(getRootFolder() + "/uploadedFiles" + folder);
			File theNewestFile = null;
			FileFilter fileFilter = new WildcardFileFilter("*.xml");
			File[] files = dir.listFiles(fileFilter);
			if (files.length > 0) {
				/** The newest file comes first **/
				Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
				theNewestFile = files[0];
			}
			return theNewestFile;

	}
	
	public static File getCounterImportLogFile(String sushiSettingURI, String id) {
		String folder = getCounterDataFolder(sushiSettingURI);
//		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();

			// get the latest (last modified!) file
			// inspired by:
			// http://stackoverflow.com/questions/285955/java-get-the-newest-file-in-a-directory
//			Resource resource = appContext.getResource("classpath:counterfiles" + folder);
			File dir = new File(getRootFolder() + "/uploadedFiles" + folder + "/logs");
			File theNewestFile = null;
			FileFilter fileFilter = new WildcardFileFilter("*.log");
			File[] files = dir.listFiles(fileFilter);
			if (files.length > 0) {
				for(int i = 0; i < files.length; i++){
					if(files[i].getName().split("_")[0].equals(id)){
						return files[i];
					}
				}
			}
			return theNewestFile;

	}

	public static List<File> getAllCounterFiles(String folder) {
		File dir = new File(getRootFolder() + "/uploadedFiles/" + folder);
		// FileFilter fileFilter = new WildcardFileFilter("*.xml");
		// File[] files = dir.listFiles(fileFilter);
		File[] files = dir.listFiles();
		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
		return Arrays.asList(files);
	}

	private static String getCounterDataFolder(String sushiSettingURI) {
		String result = null;
		VirtuosoConnector db = VirtuosoConnector.getInstance();
		String query = "select distinct ?folder from <http://amsl.technology/config/counter/> where {<" + sushiSettingURI
				+ "> <http://vocab.ub.uni-leipzig.de/terms/linkToStatsFolder> ?folder}";
		ResultSet results = db.query(query);
		while (results.hasNext()) {
			QuerySolution rs = results.nextSolution();
			RDFNode o = rs.get("folder");
			if (o.isLiteral()) {
				result = o.asLiteral().getValue().toString();
			}
		}
		return result;
	}
}