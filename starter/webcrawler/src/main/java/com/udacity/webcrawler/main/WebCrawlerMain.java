package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;
import lombok.extern.java.Log;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.Objects;

@Log

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);
    // TODO: Write the crawl results to a JSON file (or System.out if the file name is empty)
    // TODO: Write the profile data to a text file (or System.out if the file name is empty)

    if (!config.getResultPath().isEmpty()) {
      Path path = Paths.get(config.getResultPath());
      resultWriter.write(path);
    } else {
      // close the resources with try-with-resources
        try (Writer streamWriter = new BufferedWriter(new OutputStreamWriter(System.out))) {
            resultWriter.write(streamWriter);
            streamWriter.flush();
        }
    }


    // TODO: Write the profile data to a text file (or System.out if the file name is empty)
    if (!config.getProfileOutputPath().isEmpty()) {
      Path path = Paths.get(config.getProfileOutputPath());
      profiler.writeData(path);
    }else {
        // close the resources with try-with-resources
            try (Writer streamWriter = new BufferedWriter(new OutputStreamWriter(System.out))) {
                profiler.writeData(streamWriter);
                streamWriter.flush();
            }
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      log.info("Usage: WebCrawlerMain [starting-url]");
      log.info ("Service is up and running {}");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    log.info ("status: okay {}");
    new WebCrawlerMain(config).run();
  }
}
