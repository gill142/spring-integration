package com.example.filesintegration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ImageBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
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
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;


@SpringBootApplication
public class FilesIntegrationApplication {

	@Bean
	DefaultFtpSessionFactory ftpFileSessionFactory(
			@Value("${ftp.port:2121}") int port,
			@Value ("${ftp.username:kamal}") String username,
			@Value ("${ftp.password:spring}") String password)
	{
		DefaultFtpSessionFactory ftpSessionFactory = new DefaultFtpSessionFactory();
		ftpSessionFactory.setPort(port);
		ftpSessionFactory.setUsername(username);
		ftpSessionFactory.setPassword(password);
		return ftpSessionFactory;
	}

	@Bean
	IntegrationFlow files (@Value("${input-directory:${HOME}/Desktop/in}") File in,
						   Environment environment,
						   DefaultFtpSessionFactory ftpSessionFactory)
	{

		GenericTransformer<File, Message<String>> fileStringGenericTransformer = (File file) -> {
			try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				PrintStream printStream = new PrintStream(byteArrayOutputStream))
			{
				ImageBanner imageBanner = new ImageBanner(new FileSystemResource(file));
				imageBanner.printBanner(environment, getClass(), printStream);
				return MessageBuilder.withPayload(new String(byteArrayOutputStream.toByteArray()))
						.setHeader(FileHeaders.FILENAME,file.getAbsoluteFile().getName())
						.build();

			}
			catch (IOException e)
			{
				ReflectionUtils.rethrowRuntimeException(e);
			}
			return null;
		};

		return IntegrationFlows.from(Files.inboundAdapter(in).autoCreateDirectory(true).preventDuplicates(true).patternFilter("*.jpg"))
				.transform(File.class, fileStringGenericTransformer)
				.handle(Ftp.outboundAdapter(ftpSessionFactory).fileNameGenerator(new FileNameGenerator() {
					@Override
					public String generateFileName(Message<?> message) {
						Object o = message.getHeaders().get(FileHeaders.FILENAME);
						String fileName = String.class.cast (o);
						return fileName.split("\\.")[0] + ".txt";
					}
				}))
				.get();

	}


	public static void main(String[] args) {
		SpringApplication.run(FilesIntegrationApplication.class, args);
	}

}
