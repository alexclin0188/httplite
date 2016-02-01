package alexclin.httplite.cookie;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * PersistentCookieStore uses Android SharedPreferences to keep cookies used by your application.
 * This can be used by standard HttpURLConnections as well as the Volley library.
 *
 * Use example: place the following line in your Application java file:
 * CookieHandler.setDefault(new CookieManager(PersistentCookieStore.getInstance(this), CookiePolicy.ACCEPT_ORIGINAL_SERVER));
 *
 * This will set up the PersistentCookieStore as a singleton to be used by all of your HTTP connections.
 *
 * @author Trevor Summerfield
 * @see java.net.CookieStore
 *
 * Based on java.net.InMemoryCookieStore
 */

public class PersistentCookieStore implements CookieStore {

    private String COOKIE_PREFS = "CookiePrefsFile";
    private static final String COOKIE_JAR = "CUSTOM CookieJar";
    private static final String URI_LIST = "CUSTOM Uri_List";
    private static final String STRING_LIST = "CUSTOM STRING_LIST";
    private static final String TAG = "PersistentCookieStore";
    private static PersistentCookieStore instance; // Singleton
    private final SharedPreferences spePreferences;

    // the in-memory representation of cookies
    private List<HttpCookie> cookieJar = null;

    // the cookies are indexed by its domain and associated uri (if present)
    // CAUTION: when a cookie removed from main data structure (i.e. cookieJar),
    //          it won't be cleared in domainIndex & uriIndex. Double-check the
    //          presence of cookie when retrieve one form index store.
    private Map<String, List<HttpCookie>> domainIndex = null;
    private Map<URI, List<HttpCookie>> uriIndex = null;

    // use ReentrantLock instead of syncronized for scalability
    private ReentrantLock lock = null;

    private PersistentCookieStore(Context ctxContext) {

        spePreferences = ctxContext.getSharedPreferences(COOKIE_PREFS, 0);

        cookieJar = new ArrayList<>();
        domainIndex = new HashMap<>();
        uriIndex = new HashMap<>();
        lock = new ReentrantLock(false);

        loadCookies();
    }

    private PersistentCookieStore(Context ctxContenxt, String prefix) {
        this(ctxContenxt);
        this.COOKIE_PREFS = prefix + "CUSTOMCookiePrefsFile";
    }

    /**
     * Create a standard PersistentCookieStore.
     * @param ctx Context is needed for storage.
     * @return new instance.
     */
    public static PersistentCookieStore getInstance(Context ctx) {
        if (instance == null) {
            instance = new PersistentCookieStore(ctx);
        }
        return instance;
    }

    /**
     * Create a custom Cookie Store with a specified prefix for SharedPreferences.
     * @param ctx Context is needed for storage.
     * @param prefix Prefix for keys inside SharedPreferences.
     * @return new instance.
     */
    public static PersistentCookieStore getInstance(Context ctx, String prefix) {
        if (instance == null) {
            instance = new PersistentCookieStore(ctx);
        }
        return instance;
    }
    /**
     * @param cookies the list of cookies to be converted
     * @return a set of serialized string representations of the cookies.
     */
    private static Set<String> cookieListToStringSet(List<HttpCookie> cookies) {
        Set<String> cookieSet = new HashSet<String>();
        for (HttpCookie cookie : cookies) {
            try {
                cookieSet.add(toString(new SerializableCookie(cookie)));
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return cookieSet;
    }

    /**
     * @param strings the set of serialized string representations of cookies
     * @return A list of deserialized HttpCookie objects.
     */
    private static List<HttpCookie> stringSetToCookieList(Set<String> strings) {
        List<HttpCookie> cookieList = new ArrayList<>();
        for (String s : strings) {
            try {
                cookieList.add(((SerializableCookie) fromString(s)).getCookie());
            } catch (ClassNotFoundException | IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return cookieList;
    }

    /**
     * Read the object from Base64 string.
     */
    private static Object fromString(String s) throws IOException,
            ClassNotFoundException {
        byte[] data = Base64.decode(s, Base64.DEFAULT);
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }

    /**
     * Write the object to a Base64 string.
     */
    private static String toString(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return new String(Base64.encode(baos.toByteArray(), Base64.DEFAULT));
    }

    /**
     * @param uris set of URIs to be converted to strings
     * @return set of strings representing the URIs
     */
    private Set<String> URIToStringSet(Set<URI> uris) {
        Set<String> uriStrings = new HashSet<>();
        for (URI uri : uris) {
            uriStrings.add(uri.toString());
        }
        return uriStrings;
    }

    /**
     * @param strings set of strings to be parsed into URIs
     * @return set of URIs parsed from strings.
     */
    private Set<URI> StringToURISet(Set<String> strings) {
        Set<URI> uris = new HashSet<>();
        for (String s : strings) {
            uris.add(URI.create(s));
        }
        return uris;
    }

    /**
     * Store cookies from the memory of this cookieStore into SharedPreferences.
     */
    private void storeCookies() {
        lock.lock();
        // open the sharedPref editor
        SharedPreferences.Editor editor = spePreferences.edit();

        // put the entire cookie jar in the sharedPrefs
        editor.putStringSet(COOKIE_JAR, cookieListToStringSet(cookieJar));

        // put the set of URIs in there so we can get them out.
        Set<URI> uris = uriIndex.keySet();
        if (!uris.isEmpty()) editor.putStringSet(URI_LIST, URIToStringSet(uris));

        // store each set from the URI map into its own key
        for (URI uri : uris) {
            editor.putStringSet(uri.toString(), cookieListToStringSet(uriIndex.get(uri)));
        }

        // put the set of domain strings in as well.
        Set<String> domains = domainIndex.keySet();
        if (!domains.isEmpty()) editor.putStringSet(STRING_LIST, domains);

        // store each set from the domain map into its own key
        for (String domain : domains) {
            editor.putStringSet(domain, cookieListToStringSet(domainIndex.get(domain)));
        }

        editor.apply();
        lock.unlock();

    }

    /**
     * Load cookies from the SharedPreferences storage into the memory of this cookiesStore.
     */
    private void loadCookies() {
        lock.lock();
        try {
            Set<String> cookieStrings = spePreferences.getStringSet(COOKIE_JAR, null);
            if (cookieStrings != null) cookieJar.addAll(stringSetToCookieList(cookieStrings));

            Set<String> URIStrings = spePreferences.getStringSet(URI_LIST, null);
            if (URIStrings != null) {
                for (String s : URIStrings) {
                    uriIndex.put(URI.create(s), stringSetToCookieList(spePreferences.getStringSet(s, null)));
                }
            }

            Set<String> domains = spePreferences.getStringSet(STRING_LIST, null);
            if (domains != null) {
                for (String s : domains) {
                    domainIndex.put(s, stringSetToCookieList(spePreferences.getStringSet(s, null)));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to Load Cookies from SharedPref", e);
        }
        for (HttpCookie cookie : cookieJar) {
            Log.e(TAG, cookie.getName() + " : " + cookie.getValue());
        }
        lock.unlock();
    }

    /**
     * Add one cookie into cookie store.
     */
    public void add(URI uri, HttpCookie cookie) {
        // pre-condition : argument can't be null
        if (cookie == null) {
            throw new NullPointerException("cookie is null");
        }

        lock.lock();
        try {
            // remove the ole cookie if there has had one
            cookieJar.remove(cookie);

            // add new cookie if it has a non-zero max-age
            if (cookie.getMaxAge() != 0) {
                cookieJar.add(cookie);
                // and add it to domain index
                addIndex(domainIndex, cookie.getDomain(), cookie);
                // add it to uri index, too
                addIndex(uriIndex, getEffectiveURI(uri), cookie);
            }
        } finally {
            lock.unlock();
        }
        storeCookies();
    }

    /**
     * Get all cookies, which:
     * 1) given uri domain-matches with, or, associated with
     * given uri when added to the cookie store.
     * 3) not expired.
     * See RFC 2965 sec. 3.3.4 for more detail.
     */
    public List<HttpCookie> get(URI uri) {
        // argument can't be null
        if (uri == null) {
            throw new NullPointerException("uri is null");
        }

        List<HttpCookie> cookies = new ArrayList<HttpCookie>();
        lock.lock();
        try {
            // check domainIndex first
            getInternal(cookies, domainIndex, new DomainComparator(uri.getHost()));
            // check uriIndex then
            getInternal(cookies, uriIndex, getEffectiveURI(uri));
        } finally {
            lock.unlock();
        }

        return cookies;
    }

    /**
     * Get all cookies in cookie store, except those have expired
     */
    public List<HttpCookie> getCookies() {
        List<HttpCookie> rt;

        lock.lock();
        try {
            Iterator<HttpCookie> it = cookieJar.iterator();
            while (it.hasNext()) {
                if (it.next().hasExpired()) {
                    it.remove();
                }
            }
        } finally {
            rt = Collections.unmodifiableList(cookieJar);
            lock.unlock();
        }

        return rt;
    }

    /**
     * Get all URIs, which are associated with at least one cookie
     * of this cookie store.
     */
    public List<URI> getURIs() {
        List<URI> uris = new ArrayList<URI>();

        lock.lock();
        try {
            Iterator<URI> it = uriIndex.keySet().iterator();
            while (it.hasNext()) {
                URI uri = it.next();
                List<HttpCookie> cookies = uriIndex.get(uri);
                if (cookies == null || cookies.size() == 0) {
                    // no cookies list or an empty list associated with
                    // this uri entry, delete it
                    it.remove();
                }
            }
        } finally {
            uris.addAll(uriIndex.keySet());
            lock.unlock();
        }

        return uris;
    }

    /* ---------------- Private operations -------------- */

    /**
     * Remove a cookie from store
     */
    public boolean remove(URI uri, HttpCookie ck) {
        // argument can't be null
        if (ck == null) {
            throw new NullPointerException("cookie is null");
        }

        boolean modified = false;
        lock.lock();
        try {
            modified = cookieJar.remove(ck);
        } finally {
            lock.unlock();
        }

        storeCookies();
        return modified;

    }

    /**
     * Remove all cookies in this cookie store.
     */
    public boolean removeAll() {
        lock.lock();
        try {
            cookieJar.clear();
            domainIndex.clear();
            uriIndex.clear();
        } finally {
            lock.unlock();
        }
        storeCookies();
        return true;
    }

    /**
     * @param cookies     [OUT] contains the found cookies
     * @param cookieIndex the index
     * @param comparator  the prediction to decide whether or not
     *                    a cookie in index should be returned
     */
    private <T> void getInternal(List<HttpCookie> cookies,
                                 Map<T, List<HttpCookie>> cookieIndex,
                                 Comparable<T> comparator) {
        for (T index : cookieIndex.keySet()) {
            if (comparator.compareTo(index) == 0) {
                List<HttpCookie> indexedCookies = cookieIndex.get(index);
                // check the list of cookies associated with this domain
                if (indexedCookies != null) {
                    Iterator<HttpCookie> it = indexedCookies.iterator();
                    while (it.hasNext()) {
                        HttpCookie ck = it.next();
                        if (cookieJar.indexOf(ck) != -1) {
                            // the cookie still in main cookie store
                            if (!ck.hasExpired()) {
                                // don't add twice
                                if (!cookies.contains(ck))
                                    cookies.add(ck);
                            } else {
                                it.remove();
                                cookieJar.remove(ck);
                            }
                        } else {
                            // the cookie has been removed from main store,
                            // so also remove it from domain indexed store
                            it.remove();
                        }
                    }
                } // end of indexedCookies != null
            } // end of comparator.compareTo(index) == 0
        } // end of cookieIndex iteration
    }

    // add 'cookie' indexed by 'index' into 'indexStore'
    private <T> void addIndex(Map<T, List<HttpCookie>> indexStore,
                              T index,
                              HttpCookie cookie) {
        if (index != null) {
            List<HttpCookie> cookies = indexStore.get(index);
            if (cookies != null) {
                // there may already have the same cookie, so remove it first
                cookies.remove(cookie);

                cookies.add(cookie);
            } else {
                cookies = new ArrayList<HttpCookie>();
                cookies.add(cookie);
                indexStore.put(index, cookies);
            }
        }
    }

    //
    // for cookie purpose, the effective uri should only be scheme://authority
    // the path will be taken into account when path-match algorithm applied
    //
    private URI getEffectiveURI(URI uri) {
        URI effectiveURI = null;
        try {
            effectiveURI = new URI(uri.getScheme(),
                    uri.getAuthority(),
                    null,  // path component
                    null,  // query component
                    null   // fragment component
            );
        } catch (URISyntaxException ignored) {
            effectiveURI = uri;
        }

        return effectiveURI;
    }

    static class DomainComparator implements Comparable<String> {
        String host = null;

        public DomainComparator(String host) {
            this.host = host;
        }

        public int compareTo(String domain) {
            if (HttpCookie.domainMatches(domain, host)) {
                return 0;
            } else {
                return -1;
            }
        }
    }

}