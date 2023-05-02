package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final PageParserFactory parserFactory;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;

  public static Lock reentrantLock = new ReentrantLock();


  @Inject
  ParallelWebCrawler(
      Clock clock,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @TargetParallelism int threadCount,
      @IgnoredUrls List<Pattern> ignoredUrls,
      @MaxDepth int maxDepth,
      PageParserFactory parserFactory) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.ignoredUrls = ignoredUrls;
    this.maxDepth = maxDepth;
    this.parserFactory = parserFactory;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {

    Instant instant = clock.instant().plus(timeout);
    ConcurrentMap<String, Integer> quantity = new ConcurrentHashMap<>();
    ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
    for (String header : startingUrls) {
      pool.invoke(new parallelCrawlInternal(header, instant, maxDepth, quantity, visitedUrls,clock,parserFactory,ignoredUrls));
    }
    if (quantity.isEmpty()) {
      return new CrawlResult.Builder()
              .setWordCounts(quantity)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }
    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(quantity, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  public class parallelCrawlInternal extends RecursiveTask<Boolean> {
    private String url;
    private Instant deadline;
    private int maxDepth;
    private ConcurrentMap<String, Integer> counts;
    private ConcurrentSkipListSet<String> visitedUrls;
    private Clock clock;
    private PageParserFactory parserFactory;
    private List<Pattern> ignoredUrls;


    public parallelCrawlInternal(String url, Instant deadline, int maxDepth, ConcurrentMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls, Clock clock,
                                 PageParserFactory parserFactory, List<Pattern> ignoredUrls) {
      this.url = url;
      this.deadline = deadline;
      this.maxDepth = maxDepth;
      this.counts = counts;
      this.visitedUrls = visitedUrls;
      this.clock = clock;
      this.parserFactory = parserFactory;
      this.ignoredUrls = ignoredUrls;
    }

    @Override
     protected Boolean compute() {
      if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
        return false;
      }
      for (Pattern patterns : ignoredUrls) {
        if (patterns.matcher(url).matches()) {
          return false;
        }
      }
      try {
        reentrantLock.lock();
        if (visitedUrls.contains(url)) {
          return false;
        }
        visitedUrls.add(url);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        reentrantLock.unlock();
      }
      PageParser.Result result = parserFactory.get(url).parse();
      for (ConcurrentMap.Entry<String, Integer> entry : result.getWordCounts().entrySet()) {
        counts.compute(entry.getKey(), (k, v) -> (v == null) ? entry.getValue() : entry.getValue() + v);
      }
      List<parallelCrawlInternal> parallelCrawlInternals = new ArrayList<>();
      for (String link : result.getLinks()){

      parallelCrawlInternals.add(new parallelCrawlInternal(link, deadline, maxDepth -1, counts, visitedUrls, clock, parserFactory, ignoredUrls));
    }
    invokeAll(parallelCrawlInternals);
    return true;
    }
  }


    @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
