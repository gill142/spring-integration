package com.example.filesintegration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.Adapters;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.sftp.Sftp;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.ftp.dsl.Ftp;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;

import java.io.File;


@SpringBootApplication
public class FilesIntegrationApplication {

	@Bean
	DefaultFtpSessionFactory ftpFileSessionFactory(
			@Value("${ftp.port:2121}") int port,
			@Value ("${ftp.username:kamal}") String username,
			@Value ("${ftp.password:spring}") String password)
	{
		DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
		ftpSessionFactory.setPort(2121);
		ftpSessionFactory.setUsername(username);
		ftpSessionFactory.setPassword(password);
		return ftpSessionFactory;
	}

	@Bean
	IntegrationFlow files (@Value("${input-directory:${HOME}/Desktop/in}") File in,
						   DefaultFtpSessionFactory ftpSessionFactory){
		return IntegrationFlows.from(Files.inboundAdapter(in).autoCreateDirectory(true).preventDuplicates(true))
				.transform(File.class, (GenericTransformer<File, String>) (File file) -> {
						return null;
				})
				.handle(Ftp.outboundAdapter(ftpSessionFactory).fileNameGenerator(new FileNameGenerator() {
					@Override
					public String generateFileName(Message<?> message) {
						Object o = message.getHeaders().get(FileHeaders.FILENAME);
						String fileName = String.class.cast(o);
						return fileName.split("\\.")[0] + ".txt";
					}
				}))
				.

	}


	public static void main(String[] args) {
		SpringApplication.run(FilesIntegrationApplication.class, args);
	}

}
