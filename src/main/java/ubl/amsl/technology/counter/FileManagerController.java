package ubl.amsl.technology.counter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ubl.amsl.technology.counter.files.StorageException;
import ubl.amsl.technology.counter.files.StorageFileNotFoundException;
import ubl.amsl.technology.counter.files.StorageService;

@Controller
@RequestMapping("/counter/files")
public class FileManagerController {
	private final StorageService storageService;

	private final Path rootLocation;

	@Autowired
	public FileManagerController(StorageService storageService) {
		this.storageService = storageService;
		this.rootLocation = Paths.get(FileLoader.getRootFolder().toURI());
	}

	@RequestMapping(value = "/{organization}/{provider}", method = RequestMethod.GET)
	public @ResponseBody List<String> listUploadedFiles(@PathVariable String organization, @PathVariable String provider, Model model) throws IOException {
		List<String> result = new ArrayList<>();
		List<File> files = FileLoader.getAllCounterFiles(organization + "/" + provider);
		for(File file : files){
			result.add(file.getName());
		}
		return result;
	}
	
	@RequestMapping(value = "/undo/{organization}/{provider}/{file}", method = RequestMethod.GET)
	public @ResponseBody void undoImport(@PathVariable String file, @PathVariable String organization, @PathVariable String provider, Model model) throws IOException {
		SushiImporter.undoImport(file, organization, provider);
	}

	@GetMapping("/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = storageService.loadAsResource(filename);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
				.body(file);
	}

	@RequestMapping(value = "/{organization}/{provider}", method = RequestMethod.POST, consumes = {
			"multipart/form-data" })
	public String handleFileUpload(@PathVariable String organization, @PathVariable String provider,
			@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
		try {
			if (file.isEmpty()) {
				System.out.println("Failed to store empty file " + file.getOriginalFilename());
			}

			// if not existing create folder for uploaded files
			File directory = new File(String
					.valueOf(this.rootLocation.resolve(FileLoader.getRootFolder().getPath() + "/uploadedFiles/")));
			if (!directory.exists()) {
				directory.mkdir();
			}
			// if not existing create folder for organization
			directory = new File(String.valueOf(this.rootLocation
					.resolve(FileLoader.getRootFolder().getPath() + "/uploadedFiles/" + organization + "/")));
			if (!directory.exists()) {
				directory.mkdir();
			}
			// if not existing create folder for provider
			directory = new File(String.valueOf(this.rootLocation.resolve(
					FileLoader.getRootFolder().getPath() + "/uploadedFiles/" + organization + "/" + provider + "/")));
			if (!directory.exists()) {
				directory.mkdir();
			}
			// write file
			String destination = this.rootLocation.resolve(FileLoader.getRootFolder().getPath() + "/uploadedFiles/" + organization + "/"
					+ provider + "/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename()).toString();
			Files.copy(file.getInputStream(), this.rootLocation.resolve(destination));
			// start counter import
			SushiImporter.processActual(this.rootLocation.resolve(destination).toFile(), organization, provider);
		} catch (IOException e) {
			e.printStackTrace();
			throw new StorageException("Failed to store file " + file.getOriginalFilename(), e);
		}

		redirectAttributes.addFlashAttribute("message",
				"You successfully uploaded " + file.getOriginalFilename() + "!");

		return "redirect:/";
	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}
}
