package uk.bl.monitrix.model;

import java.util.Iterator;
import java.util.List;

/**
 * The crawl log interface.
 * @author Rainer Simon <rainer.simon@ait.ac.at>
 */
public abstract class CrawlLog {
	
	/**
	 * Returns the UNIX timestamp of the crawl start time, i.e.
	 * the timestamp of the first entry written to the log.
	 * @return crawl start time
	 */
	public abstract long getCrawlStartTime();
	
	/**
	 * Returns the UNIX timestamp of the last crawl activity, i.e.
	 * the timestamp of the last entry in written to the log.
	 * @return last crawl activity timestamp
	 */
	public abstract long getTimeOfLastCrawlActivity();
	
	/**
	 * Utility method: returns the duration of the crawl so far (in
	 * milliseconds).
	 * @return the duration of the crawl
	 */
	public long getCrawlDuration() {
		return getTimeOfLastCrawlActivity() - getCrawlStartTime();
	}
	
	/**
	 * Utility method: returns true if the last crawl activity was
	 * more than 2 minutes ago (in which case we consider the crawl idle)
	 * @return <code>true</code> if the crawl is idle 
	 */
	public boolean isIdle() {
		return (System.currentTimeMillis() - getTimeOfLastCrawlActivity()) > 120000; 
	}

	/**
	 * Returns the N most recent entries in the log.
	 * @param n the number of entries to return
	 * @return the log entries
	 */
	public abstract List<CrawlLogEntry> getMostRecentEntries(int n);
	
	/**
	 * Returns the total number of log entries.
	 * @return the total number of log entries
	 */
	public abstract long countEntries();
	
	/**
	 * Counts the log entries for a specific host.
	 * @param hostname the host name
	 * @return the number of log entries for the host
	 */
	public abstract long countEntriesForHost(String hostname);
	
	/**
	 * Returns the log entries for a specific host. 
	 * @param hostname the host name
	 * @return the log entries for the host
	 */
	public abstract Iterator<CrawlLogEntry> getEntriesForHost(String hostname); 
	
}
