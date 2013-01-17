package uk.bl.monitrix.database.mongodb.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import uk.bl.monitrix.database.mongodb.MongoProperties;
import uk.bl.monitrix.model.CrawlLog;
import uk.bl.monitrix.model.CrawlLogEntry;

/**
 * A MongoDB-backed implementation of {@link CrawlLog}.
 * @author Rainer Simon <rainer.simon@ait.ac.at>
 *
 */
public class MongoCrawlLog extends CrawlLog {
	
	protected DBCollection collection;
	
	private static DBObject PROJECTION;
	
	static {
		PROJECTION = new BasicDBObject(); 
		PROJECTION.put(MongoProperties.FIELD_CRAWL_LOG_CRAWLER_ID, 1);
		PROJECTION.put(MongoProperties.FIELD_CRAWL_LOG_HTTP_CODE, 1);
		PROJECTION.put(MongoProperties.FIELD_CRAWL_LOG_ANNOTATIONS, 1);
		PROJECTION.put(MongoProperties.FIELD_CRAWL_LOG_LINE, 1);
	}
	
	public MongoCrawlLog(DB db) {
		this.collection = db.getCollection(MongoProperties.COLLECTION_CRAWL_LOG);
		
		// The Heritrix Log collection is indexed by timestamp and hostname (will be skipped automatically if index exists)
		this.collection.ensureIndex(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_TIMESTAMP, 1));
		this.collection.ensureIndex(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_HOST, 1));
	}

	@Override
	public long getCrawlStartTime() {
		// TODO cache
		long crawlStartTime = 0;
		DBCursor cursor = collection.find().limit(1).sort(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_TIMESTAMP, 1));
		while (cursor.hasNext())
			crawlStartTime = new MongoCrawlLogEntry(cursor.next()).getTimestamp().getTime();					
		
		return crawlStartTime;
	}

	@Override
	public long getTimeOfLastCrawlActivity() {
		// TODO cache
		long lastCrawlActivity = 0;
		DBCursor cursor = collection.find().limit(1).sort(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_TIMESTAMP, -1));
		while (cursor.hasNext())
			lastCrawlActivity = new MongoCrawlLogEntry(cursor.next()).getTimestamp().getTime();					
		
		return lastCrawlActivity;
	}

	@Override
	public List<CrawlLogEntry> getMostRecentEntries(int n) {
		DBCursor cursor = collection.find().sort(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_TIMESTAMP, -1)).limit(n);
		
		List<CrawlLogEntry> recent = new ArrayList<CrawlLogEntry>();
		while(cursor.hasNext())
			recent.add(new MongoCrawlLogEntry(cursor.next()));

		return recent;
	}
	
	@Override
	public long countEntries() {
		return collection.count();
	}

	@Override
	public long countEntriesForHost(String hostname) {
		return collection.count(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_HOST, hostname));
	}

	@Override
	public Iterator<CrawlLogEntry> getEntriesForHost(String hostname) {
		return getEntriesForHost(hostname, false);
	}
	
	/**
	 * An internal helper method that can return full as well as truncated versions of the log entries.
	 * Truncated log entries only contain crawler ID, HTTP fetch code, annotations
	 * The {@link Hosts} controller uses the truncated version in order to gain a slight speed improvement
	 * over the full log entries.
	 * @param hostname the hostname
	 * @param truncated if <code>true</code> the log entries will only contain a subset of the fields.
	 * @return
	 */
	public Iterator<CrawlLogEntry> getEntriesForHost(String hostname, boolean truncated) {
		long limit = collection.count(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_HOST, hostname));		
		
		// We're using a count first to improve performance (?)
		// Cf. http://docs.mongodb.org/manual/applications/optimization/
		final DBCursor cursor = (truncated) ? collection
				.find(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_HOST, hostname), PROJECTION)
				.hint(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_HOST, 1))
				.limit((int) limit)
			: collection
				.find(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_HOST, hostname))
				.hint(new BasicDBObject(MongoProperties.FIELD_CRAWL_LOG_HOST, 1))
				.limit((int) limit);	
		
		return new Iterator<CrawlLogEntry>() {
			@Override
			public boolean hasNext() {
				return cursor.hasNext();
			}

			@Override
			public CrawlLogEntry next() {
				return new MongoCrawlLogEntry(cursor.next());	
			}

			@Override
			public void remove() {
				cursor.remove();
			}
		};
	}

}
