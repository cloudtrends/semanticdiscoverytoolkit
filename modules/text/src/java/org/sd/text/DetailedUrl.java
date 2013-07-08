/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.text;


import org.sd.nlp.GeneralNormalizedString;
import org.sd.util.PathWrapper;
import org.sd.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility wrapper around a url that gives detailed access to its pieces.
 * <p>
 * Where detailed pieces are:
 * <p>
 * (for http://www.semanticdiscovery.com:8080/foo/bar/baz.html?a=b#anchor1)
 * <ul>
 * <li>protocol -- i.e. http://</li>
 * <li>hostPrefix -- i.e. www.</li>
 * <li>host -- i.e. semanticdiscovery.com</li>
 * <li>port -- i.e. :8080</li>
 * <li>path -- i.e. /foo/bar/</li>
 * <li>target -- i.e. baz</li>
 * <li>targetExtension -- i.e. .html</li>
 * <li>query -- i.e. ?a=b</li>
 * <li>anchor -- i.e. #anchor1</li>
 * </ul>
 *
 * @author Spence Koehler
 */
public class DetailedUrl {
  
  /**
   * Normalize the url string.
   */
  public static final String normalize(String urlString) {
    return new DetailedUrl(urlString).getNormalizedUrl();
  }

  /**
   * Cleanup a url string by:
   * <ul>
   * <li>Removing whitespace, single and double quotes, and less than/greater than signs.</li>
   * </ul>
   */
  public static final String cleanup(String urlString) {
    final StringBuilder result = new StringBuilder();

    if (urlString != null) {
      for (StringUtil.StringIterator iter = new StringUtil.StringIterator(urlString); iter.hasNext(); ) {
        StringUtil.StringPointer pointer = iter.next();
        final int cp = pointer.codePoint;

        // squash ' ', '"', '\'', '>'
        if (cp == ' ' || cp == '"' || cp == '\'' || cp == '<' || cp == '>') continue;

        result.appendCodePoint(cp);
      }    
    }

    return result.toString();
  }

  /**
   * Assuming a string with just the "host", clean off any extra
   * chars at the end.
   * <p>
   * Note: currently, we strip off any "-JUNK" at the end, where
   * the dash occurs after the last dot.
   */
  public static final String cleanupHost(String host) {
    String result = host;

    // remove the hash code (inserted by the crawler) from the end of a domain if present
    // i.e. "foo.com-<hashcode>" should become "foo.com"
    final int dashPos = result.lastIndexOf('-');
    final int dotPos = result.lastIndexOf('.');
    if (dashPos > dotPos) {
      result = result.substring(0, dashPos);
    }
    
    return result;
  }


  public static final Set<String> HOST_EXTENSIONS = new HashSet<String>();
  private static final String[] HE = new String[] {
    "ac", "ad", "ae", "af", "ag", "ai", "al", "am", "an", "ao", "aq", "ar",
    "arpa", "as", "at", "au", "aw", "az", "ba", "bb", "bd", "be", "bf", "bg",
    "bh", "bi", "bj", "bm", "bn", "bo", "br", "bs", "bt", "bv", "bw", "by",
    "bz", "ca", "cc", "cf", "cg", "ch", "ci", "ck", "cl", "cm", "cn", "co",
    "com", "cr", "cs", "cu", "cv", "cx", "cy", "cz", "de", "dj", "dk", "dm",
    "do", "dz", "ec", "edu", "ee", "eg", "eh", "er", "es", "et", "fi", "firm",
    "fj", "fk", "fm", "fo", "fr", "fx", "ga", "gb", "gd", "ge", "gf", "gh",
    "gi", "gl", "gm", "gn", "gov", "gp", "gq", "gr", "gs", "gt", "gu", "gw",
    "gy", "hk", "hm", "hn", "hr", "ht", "hu", "id", "ie", "il", "in", "int",
    "io", "iq", "ir", "is", "it", "jm", "jo", "jp", "ke", "kg", "kh", "ki",
    "km", "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc", "li", "lk",
    "lr", "ls", "lt", "lu", "lv", "ly", "ma", "mc", "md", "mg", "mh", "mil",
    "mk", "ml", "mm", "mn", "mo", "mp", "mq", "mr", "ms", "mt", "mu", "mv",
    "mw", "mx", "my", "mz", "na", "nato", "nc", "ne", "net", "nf", "ng", "ni",
    "nl", "no", "nom", "np", "nr", "nt", "nu", "nz", "om", "org", "pa", "pe",
    "pf", "pg", "ph", "pk", "pl", "pm", "pn", "pr", "pt", "pw", "py", "qa",
    "re", "ro", "ru",   "rw", "sa", "sb", "sc", "sd", "se", "sg", "sh", "si",
    "sj", "sk", "sl", "sm", "sn", "so", "sr", "st", "store", "su", "sv", "sy",
    "sz", "tc", "td", "tf", "tg", "th", "tj", "tk", "tm", "tn", "to", "tp",
    "tr", "tt", "tv", "tw", "tz", "ua", "ug", "uk", "um", "us", "uy", "uz",
    "va", "vc", "ve", "vg", "vi", "vn", "vu", "web", "wf", "ws", "ye", "yt",
    "yu", "za", "zm", "zr", "zw", "biz", "info",
  };
  static {
    for (String he : HE) {
      HOST_EXTENSIONS.add(he);
    }
  }


  private static final int FIRST_COLON_INDEX = 0;    // before first '.', incl ://
  private static final int SECOND_COLON_INDEX = 1;   // after first colon index
  private static final int FIRST_DOT_INDEX = 2;
  private static final int LAST_DOT_INDEX = 3;       // before '?' or '#'
  private static final int FIRST_SLASH_INDEX = 4;    // after "://", before '?' or '#'
  private static final int LAST_SLASH_INDEX = 5;     // before '?' or '#'
  private static final int FIRST_QUESTION_INDEX = 6; // if no '?', look for '&'
  private static final int LAST_HASH_INDEX = 7;
  private static final int EOS_INDEX = 8;
  private static final int NUM_INDEXES = 9;


  private static final int PROTOCOL_MARKER = 0;
  private static final int HOST_PREFIX_MARKER = 1;
  private static final int PORT_MARKER = 2;
  private static final int PATH_START_MARKER = 3;
  private static final int PATH_END_MARKER = 4;
  private static final int TARGET_EXT_MARKER = 5;
  private static final int QUERY_MARKER = 6;
  private static final int ANCHOR_MARKER = 7;
  private static final int NUM_MARKERS = 8;


  private static final String EMPTY = "";


  private String urlString;

  // parsed pieces
  private String _protocol;
  private String _hostPrefix;
  private String _host;
  private String _port;
  private String _path;
  private String _target;
  private String _targetExtension;
  private String _query;
  private String _anchor;

  // normalized versions
  private String _nProtocol;
  private String _nHostPrefix;
  private String _nHost;
  private String _nTargetExtension;
  private String _nUrlString;
  private String _anUrlString;
  private String _nqUrlString;
  private String _nnaUrlString;
  private URL _url;
  private MalformedURLException _urlError;

  // intermediate data
  private int[] _indexes;
  private int[] _markers;
  private String[] _hostExtensions;
  private String[] _domainNameWords;
  private Set<String> _urlPathWords;
  private String _actualPath;
  private PathWrapper _pathWrapper;
  private String _urlPath;

  /**
   * Construct with a url string.
   */
  public DetailedUrl(String urlString) {
    this.urlString = cleanup(urlString);
  }

  /**
   * Get the protocol portion of this url.
   * <p>
   * For example, "http://", "file://", "https://", "ftp://", "news:"
   * <p>
   * If normalized, lowercase and replace backslashes with forward slashes.
   */
  public String getProtocol(boolean normalized) {
    if (_protocol == null) {
      final int[] markers = getMarkers();
      _protocol = getSubString(0, markers[PROTOCOL_MARKER] + 1);
    }

    if (normalized && _nProtocol == null) {
      _nProtocol = _protocol.toLowerCase().replaceAll("\\\\", "/");
    }

    return normalized ? _nProtocol : _protocol;
  }

  private static final Pattern[] PREFIX_PATTERNS = new Pattern[] {
    Pattern.compile("^[Ww][Ww][Ww]-?\\d*\\.$"),
    Pattern.compile("^[Ww][Ee][Bb]-?\\d*\\.$"),
  };

  /**
   * Get the host prefix portion of the host (with trailing '.'), if recognized.
   * <p>
   * For example, "www.", "www1.", "web.", "web1.", etc.
   * <p>
   * If normalized, lowercase.
   */
  public String getHostPrefix(boolean normalized) {
    if (_hostPrefix == null) {
      final int[] markers = getMarkers();
      _hostPrefix = getSubString(markers[PROTOCOL_MARKER] + 1, markers[HOST_PREFIX_MARKER] + 1);

      if (!EMPTY.equals(_hostPrefix)) {
        boolean isValid = false;

        // verify it matches a prefix pattern.
        for (Pattern pattern : PREFIX_PATTERNS) {
          final Matcher m = pattern.matcher(_hostPrefix);
          if (m.matches()) {
            isValid = true;
            break;
          }
        }

        if (!isValid) _hostPrefix = EMPTY;
      }
    }

    if (normalized && _nHostPrefix == null) {
      _nHostPrefix = _hostPrefix.toLowerCase();
    }

    return normalized ? _nHostPrefix : _hostPrefix;
  }

  /**
   * Get the host with or without the host prefix and/or port.
   * <p>
   * If normalized, lowercase.
   */
  public String getHost(boolean normalized, boolean withHostPrefix, boolean withPortIfPresent) {
    if (_host == null) {
      final int[] markers = getMarkers();
      final int startIndex = EMPTY.equals(getHostPrefix(false)) ? markers[PROTOCOL_MARKER] + 1 : markers[HOST_PREFIX_MARKER] + 1;

      final int[] indexes = getIndexes();
      final int endIndex = minIndex(new int[]{markers[PORT_MARKER], markers[PATH_START_MARKER], markers[QUERY_MARKER], markers[ANCHOR_MARKER], indexes[EOS_INDEX]});

      _host = cleanupHost(getSubString(startIndex, endIndex));
    }

    if (normalized && _nHost == null) {
      _nHost = _host.toLowerCase();
    }

    String result = normalized ? _nHost : _host;

    if (withHostPrefix) {
      result = getHostPrefix(normalized) + result;
    }
    if (withPortIfPresent) {
      result = result + getPort();
    }

    return result;
  }

  /**
   * Get the index of the start of the host after the host prefix.
   */
  public int getPostPrefixHostIndex() {
    final int[] markers = getMarkers();
    return EMPTY.equals(getHostPrefix(false)) ? markers[PROTOCOL_MARKER] + 1 : markers[HOST_PREFIX_MARKER] + 1;
  }

  /**
   * Get the port (if present) with its preceding colon (':') or an empty string.
   */
  public String getPort() {
    if (_port == null) {
      final int[] markers = getMarkers();

      if (markers[PORT_MARKER] >= 0) {
        final int[] indexes = getIndexes();
        final int endIndex = minIndex(new int[]{markers[PATH_START_MARKER], markers[QUERY_MARKER], markers[ANCHOR_MARKER], indexes[EOS_INDEX]});

        _port = getSubString(markers[PORT_MARKER], endIndex);
      }
      else _port = EMPTY;
    }
    return _port;
  }

  /**
   * Get the actual path without target or extension.
   */
  private final String getActualPath() {
    if (_actualPath == null) {
      _actualPath = computePath(false);
    }
    return _actualPath;
  }

  private final String getUrlPath() {
    if (_urlPath == null) {
      _urlPath = computePath(true);
    }
    return _urlPath;
  }

  private final String computePath(boolean relaxTrailingSlash) {
    String result = null;

    final int[] markers = getMarkers();

    boolean needsTrailingSlash = false;
    int endIndex = markers[PATH_END_MARKER] + 1;
    if (markers[TARGET_EXT_MARKER] < 0 && markers[QUERY_MARKER] < 0 && markers[ANCHOR_MARKER] < 0) {
      final int[] indexes = getIndexes();
      endIndex = indexes[EOS_INDEX];
      needsTrailingSlash = !relaxTrailingSlash;
    }

    result = getSubString(markers[PATH_START_MARKER], endIndex);

    if (result.length() > 0) {
      if (needsTrailingSlash && result.charAt(result.length() - 1) != '/') {
        result = result + "/";
      }
    }
    else {
      // if no path and have protocol, then set path to "/" for normalization
      if (getProtocol(false).length() > 0) {
        result = "/";
      }
    }
//todo: convert backslashes to forward slashes?

    return result;
  }

  private PathWrapper getPathWrapper() {
    if (_pathWrapper == null) {
      _pathWrapper = new PathWrapper(getActualPath());
    }
    return _pathWrapper;
  }

  /**
   * Get the path part of this url optionally without the target or extension.
   * <p>
   * The path (when non-empty) always has its preceding slash and, when
   * retrieved without its target or extension, will have its trailing slash.
   *
   * @return the path or an empty string if not present.
   */
  public String getPath(boolean withoutTargetOrExtension) {
    if (_path == null) {
      _path = getUrlPath();
//      _path = getPathWrapper().getPath();
    }

    return withoutTargetOrExtension ? _path : (_path + getTarget(true));
  }

  /**
   * Get the target (optionally with its extension).
   * <p>
   * The target is the final (pathless) filename referenced by the url without
   * a preceding slash.
   */
  public String getTarget(boolean withTargetExtension) {
    if (_target == null) {
      final int[] markers = getMarkers();

      if (markers[PATH_END_MARKER] >= 0) {
        int endIndex = markers[TARGET_EXT_MARKER];
        if (endIndex < 0) endIndex = markers[QUERY_MARKER];
        _target = getSubString(markers[PATH_END_MARKER] + 1, endIndex);
      }
      else _target = EMPTY;
    }

    return withTargetExtension ? (_target + getTargetExtension(false)) : _target;
  }

  /**
   * Get the extension of the url's target.
   * <p>
   * For example, ".htm", ".html", ".txt", ".jsp", etc.
   */
  public String getTargetExtension(boolean normalized) {
    if (_targetExtension == null) {
      final int[] markers = getMarkers();

      if (markers[PATH_END_MARKER] >= 0 && markers[TARGET_EXT_MARKER] >= 0) {
        final int[] indexes = getIndexes();
        final int endIndex = minIndex(new int[]{markers[QUERY_MARKER], markers[ANCHOR_MARKER], indexes[EOS_INDEX]});
        
        _targetExtension = getSubString(markers[TARGET_EXT_MARKER], endIndex);
      }
      else _targetExtension = EMPTY;
    }

    if (normalized && _nTargetExtension == null) {
      _nTargetExtension = _targetExtension.toLowerCase();
    }

    return normalized ? _nTargetExtension : _targetExtension;
  }

  /**
   * Get the query portion of the url.
   */
  public String getQuery() {
    if (_query == null) {
      final int[] markers = getMarkers();

      if (markers[QUERY_MARKER] >= 0) {
        final int[] indexes = getIndexes();
        final int endIndex = minIndex(new int[]{markers[ANCHOR_MARKER], indexes[EOS_INDEX]});

        _query = getSubString(markers[QUERY_MARKER], endIndex);
      }
      else _query = EMPTY;
    }
    return _query;
  }

  /**
   * Get the query portion of the url.
   */
  public Map<String, String> getQueryMap() {
    Map<String, String> results = new HashMap<String, String>();
    String queryString = getQuery();
    String[] queryPairs = queryString.split("&");
    
    for(String pair : queryPairs){
      String[] keyValue = pair.split("=");
      String key = null; 
      String value = null;

      if(keyValue.length > 0) key = keyValue[0];
      if(keyValue.length > 1) value = keyValue[1];
      
      if(key != null) results.put(key, value);
    }

    return results;
  }

  /**
   * Get the anchor portion of the url.
   */
  public String getAnchor() {
    if (_anchor == null) {
      final int[] markers = getMarkers();

      if (markers[ANCHOR_MARKER] >= 0) {
        _anchor = urlString.substring(markers[ANCHOR_MARKER]);
      }
      else _anchor = EMPTY;
    }
    return _anchor;
  }

  /**
   * Get the fully normalized url.
   */
  public String getNormalizedUrl() {
    if (_nUrlString == null) {
      final StringBuilder builder = new StringBuilder();
      builder.
        append(getNormalizedUrlNoAnchor()).
        append(getAnchor());
      _nUrlString = builder.toString();
    }
    return _nUrlString;
  }

  /**
   * Get the fully normalized url, but without the anchor.
   */
  public String getNormalizedUrlNoAnchor() {
    if (_nnaUrlString == null) {
      final StringBuilder builder = new StringBuilder();
      builder.
        append(getProtocol(true)).
        append(getHost(true, true, true)).
        append(getPath(false)).
        append(getQuery());
      _nnaUrlString = builder.toString();
    }
    return _nnaUrlString;
  }

  /**
   * Get the normalized url without the protocol, host prefix, or port.
   */
  public String getAbbreviatedNormalizedUrl() {
    if (_anUrlString == null) {
      final StringBuilder builder = new StringBuilder();
      builder.
        append(getHost(true, false, false)).
        append(getPath(false)).
        append(getQuery()).
        append(getAnchor());
      _anUrlString = builder.toString();
    }
    return _anUrlString;
  }

  public String getNonQueryNormalizedUrl() {
    if (_nqUrlString == null) {
      final StringBuilder builder = new StringBuilder();
      builder.
        append(getHost(true, false, false)).
        append(getPath(false));
      _nqUrlString = builder.toString();
    }
    return _nqUrlString;
  }

  /**
   * Get this url as a URL instance, assuming 'http' protocol if absent.
   */
  public URL asUrl() {
    // NOTE: the trailing '/' added for normalization purposes in some cases
    // "breaks" crawling so, for the purposes of an actual URL, this trailing
    // slash is not included.

    if (_url == null) {
      final StringBuilder builder = new StringBuilder();
      String protocol = getProtocol(true);
      if (protocol == null || "".equals(protocol)) {
        protocol = "http://";
      }

      builder.
        append(protocol).
        append(getHost(true, true, true)).
        append(getUrlPath()).
        append(getTarget(true)).
        append(getQuery()).
        append(getAnchor());

      try {
        _url = new URL(builder.toString());
      }
      catch (MalformedURLException e) {
        _urlError = e;
        _url = null;
      }
    }
    return _url;
  }

  /**
   * Get the Url Error, or null.
   */
  public MalformedURLException getUrlError() {
    if (_urlError == null && _url == null) {
      asUrl();  // force compute
    }
    return _urlError;
  }

  /**
   * Combine this url as a base or reference path with the given href
   * path to create a normalized absolute path.
   * <p>
   * NOTE that multiple consecutive "/" symbols in the path are treated as a
   * single path delimiter instead of as redirection of the path to root.
   */
  public String fixHref(String href) {
    return fixHref(new DetailedUrl(href));
  }

  /**
   * Combine this url as a base or reference path with the given href
   * path to create a normalized absolute path.
   * <p>
   * NOTE that multiple consecutive "/" symbols in the path are treated as a
   * single path delimiter instead of as redirection of the path to root.
   */
  public String fixHref(DetailedUrl href) {
    String result = null;

    if (!"".equals(href.getHost(false, false, false))) {
      result = href.getNormalizedUrl();
    }
    else {
      final StringBuilder builder = new StringBuilder();

      final PathWrapper pathWrapper = getPathWrapper();
      builder.
        append(getProtocol(true)).
        append(getHost(true, true, true)).
        append(pathWrapper.getCombined(href.getActualPath())).
        append('/').
        append(href.getTarget(true)).
        append(href.getQuery()).
        append(href.getAnchor());

      result = builder.toString();
    }

    return result;
  }

  /**
   * Get the original (input) url used to construct this instance.
   */
  public String getOriginal() {
    return urlString;
  }

  /**
   * Get this url as a string.
   * 
   * @return the fully normalized url.
   */
  public String toString() {
    return getNormalizedUrl();
  }


  private final String getSubString(int startIndex, int endIndex) {
    String result = EMPTY;

    if (startIndex < endIndex && startIndex >= 0 && endIndex >= 0) {
      result = urlString.substring(startIndex, endIndex);
    }

    return result;
  }

  private int[] getIndexes() {
    if (_indexes == null) {
      _indexes = new int[NUM_INDEXES];

      _indexes[FIRST_DOT_INDEX] = urlString.indexOf('.');
      _indexes[FIRST_QUESTION_INDEX] = urlString.indexOf('?');
      if (_indexes[FIRST_QUESTION_INDEX] < 0) _indexes[FIRST_QUESTION_INDEX] = urlString.indexOf('&');
      _indexes[LAST_HASH_INDEX] = urlString.lastIndexOf('#');
      _indexes[EOS_INDEX] = urlString.length();

      _indexes[FIRST_COLON_INDEX] = findFirstColonIndex(_indexes[FIRST_DOT_INDEX], _indexes[EOS_INDEX]);
      _indexes[FIRST_SLASH_INDEX] = findFirstSlashIndex(_indexes[FIRST_COLON_INDEX], _indexes[FIRST_QUESTION_INDEX], _indexes[LAST_HASH_INDEX]);
      _indexes[LAST_SLASH_INDEX] = urlString.lastIndexOf('/', minIndex(new int[]{_indexes[FIRST_QUESTION_INDEX], _indexes[LAST_HASH_INDEX], _indexes[EOS_INDEX]}));
      _indexes[LAST_DOT_INDEX] = urlString.lastIndexOf('.', minIndex(new int[]{_indexes[FIRST_QUESTION_INDEX], _indexes[LAST_HASH_INDEX], _indexes[EOS_INDEX]}));
      _indexes[SECOND_COLON_INDEX] = urlString.indexOf(':', _indexes[FIRST_COLON_INDEX] + 1);
    }
    return _indexes;
  }

  private final int findFirstColonIndex(int firstDotIndex, int eosIndex) {
    int result = urlString.indexOf(':');

    if (result >= 0) {
      if (result > firstDotIndex) {
        // there isn't a protocol colon! this is probably the port.
        result = -1;
      }
      else {
        // skootch over to the end of the trailing "//" if it exists.
        if (eosIndex > result + 2) {
          final int c2 = urlString.charAt(result + 2);
          if (c2 == '/' || c2 == '\\') {
            final int c1 = urlString.charAt(result + 1);
            if (c1 == '/' || c1 == '\\') {
              result += 2;
            }
          }
        }
      }
    }

    return result;
  }

  private final int findFirstSlashIndex(int firstColonIndex, int firstQuestionIndex, int lastHashIndex) {
    int result = urlString.indexOf('/', firstColonIndex + 1);  // look after the "first colon"

    if (result >= 0) {
      // only accept if before existing '?' and '#'
      if ((firstQuestionIndex >= 0 && result > firstQuestionIndex) ||
          (lastHashIndex >= 0 && result > lastHashIndex)) {
        result = -1;
      }
    }

    return result;
  }

  /**
   * Find the minimum non-negative index.
   */
  private final int minIndex(int[] indexes) {
    int result = -1;

    for (int index : indexes) {
      if ((index >= 0) && ((result < 0) || (index < result))) {
        result = index;
      }
    }

    return result;
  }


  private int[] getMarkers() {
    if (_markers == null) {
      _markers = new int[NUM_MARKERS];
      final int[] indexes = getIndexes();

      _markers[PROTOCOL_MARKER] = indexes[FIRST_COLON_INDEX];
      _markers[HOST_PREFIX_MARKER] = preIndex(indexes[FIRST_DOT_INDEX], indexes[FIRST_SLASH_INDEX]);
      _markers[PORT_MARKER] = preIndex(indexes[SECOND_COLON_INDEX], indexes[FIRST_SLASH_INDEX]);
      _markers[PATH_START_MARKER] = indexes[FIRST_SLASH_INDEX];
      _markers[PATH_END_MARKER] = postIndex(indexes[LAST_SLASH_INDEX], indexes[FIRST_SLASH_INDEX]);
      _markers[TARGET_EXT_MARKER] = postIndex(indexes[LAST_DOT_INDEX], indexes[LAST_SLASH_INDEX]);
      _markers[QUERY_MARKER] = indexes[FIRST_QUESTION_INDEX];
      _markers[ANCHOR_MARKER] = indexes[LAST_HASH_INDEX];

      // special case: if path_start_marker has a '.' before it, backup over all consecutive preceding '.'s
      int pathStart = _markers[PATH_START_MARKER];
      while (pathStart > 0 && urlString.charAt(pathStart - 1) == '.') {
        --pathStart;
      }
      _markers[PATH_START_MARKER] = pathStart;
    }
    return _markers;
  }

  /**
   * Return index1 as long as it is less than existing index2.
   * <p>
   * If index2 doesn't exist, index1 is ok.
   */
  private final int preIndex(int index1, int index2) {
    int result = index1;

    if (result >= 0) {
      if (index2 >= 0 && result > index2) {
        result = -1;
      }
    }

    return result;
  }

  /**
   * Return index1 as long as it is greater than or equal to existing index2.
   * <p>
   * If index2 doesn't exist, index1 cannot exist.
   */
  private final int postIndex(int index1, int index2) {
    int result = index1;

    if (result >= 0) {
      if (index2 < 0 || result < index2) {
        result = -1;
      }
    }

    return result;
  }

  /**
   * Split host (locator) extensions from the host (like ".co.uk" or ".gov"),
   * returning {host, extensions}.
   * <p>
   * For example: for "www.foo.co.uk", return {"foo", "co.uk"}.
   */
  public final String[] splitHostExtensions(boolean normalized) {
    if (_hostExtensions == null) {
      final String host = getHost(normalized, false, false);
      _hostExtensions = doHostExtensionsSplit(host);
    }
    return _hostExtensions;
  }

  /**
   * Auxiliary method to split extensions from the host (like ".co.uk" or ".gov"),
   * returning {host, extensiosn}.
   */
  public static String[] doHostExtensionsSplit(String host) {
    final String[] pieces = host.split("\\.");

    int boundary = pieces.length - 1;  // right-boundary for host (inclusive)
    for (; boundary >= 0; --boundary) {
      if (!HOST_EXTENSIONS.contains(pieces[boundary].toLowerCase())) {
        break;
      }
    }

    return new String[] {
      StringUtil.concat(pieces, ".", 0, boundary + 1),
      StringUtil.concat(pieces, ".", boundary + 1, pieces.length),
    };
  }

  public String[] getDomainNameWords(boolean normalized) {
    if (_domainNameWords == null) {
//todo: caching this is a "bug" in that 'normalized' will be ignored after the first access! fix it!
      final String[] xhost = splitHostExtensions(normalized);
      _domainNameWords = new GeneralNormalizedString(xhost[0]).split();
    }
    return _domainNameWords;
  }

  public Set<String> getUrlPathWords() {
    if (_urlPathWords == null) {
      _urlPathWords = new HashSet<String>();

      final String[] pathWords = new GeneralNormalizedString(getPath(false)).split();
      for (String pathWord : pathWords) _urlPathWords.add(pathWord);

      final String[] targetWords = new GeneralNormalizedString(getTarget(false)).split();
      for (String targetWord : targetWords) _urlPathWords.add(targetWord);
    }
    return _urlPathWords;
  }


  /**
   * Default hosts to ignore in extractUrls.
   */
  private static final String[] STOP_HOSTS_STRINGS = new String[] {
    "w3",
  };
  private static final Set<String> STOP_HOSTS = new HashSet<String>();
  static {
    for (String stopHost : STOP_HOSTS_STRINGS) {
      STOP_HOSTS.add(stopHost);
    }
  }


  /**
   * Extract (unique) url strings from the text.
   * <p>
   * Note that this searches the text for the pattern https?:// to find and
   * extract all urls, accounting for delimiters likely to be found in html
   * text.
   */
  public final Set<DetailedUrl> extractUrls(String text, boolean discardInternal) {
    if (text == null || "".equals(text)) return null;

    final Set<DetailedUrl> result = new HashSet<DetailedUrl>();
    boolean gotOne = extractUrls(text, result, discardInternal);

    return gotOne ? result : null;
  }

  /**
   * Extract (unique) url strings from the text into the given result.
   * <p>
   * Note that this searches the text for the pattern https?:// to find and
   * extract all urls, accounting for delimiters likely to be found in html
   * text.
   */
  public final boolean extractUrls(String text, Set<DetailedUrl> result, boolean discardInternal) {
    final int len = text.length();
    final int llen = len - 7;
    final String lcText = text.toLowerCase();
    final String host = splitHostExtensions(true)[0];
    boolean foundOne = false;
    int curPos = 0;

    while (curPos < llen) {
      final int httpPos = lcText.indexOf("http", curPos);            // find http
      if (httpPos < 0 || httpPos + 7 >= len) break;
      boolean gotOne = false;

      int checkPos = httpPos + 4;
      if (lcText.charAt(checkPos) == 's') ++checkPos;                // include https://
      if ("://".equals(lcText.substring(checkPos, checkPos + 3))) {  // check for ://
        // found one ... find its end boundary and add.

        char endDelim = 0;

        // determine valid end delimiters
        final char preChar = (httpPos > 0) ? lcText.charAt(httpPos - 1) : 0;
        if (preChar == '\'' || preChar == '"') endDelim = preChar;

        // scan forward until we see an end delimiter (end of string, white, or endDelim)
        int endPos = checkPos + 3;
        while (endPos < len) {
          final char c = lcText.charAt(endPos);
          if (c == ' ' || c == endDelim) break;
          ++endPos;
        }

        if (endPos - checkPos + 3 > 0) {
          final String urlString = text.substring(httpPos, endPos);
          final DetailedUrl refUrl = new DetailedUrl(urlString);

          final String refHost = refUrl.splitHostExtensions(true)[0];
          if (!STOP_HOSTS.contains(refHost) &&
              (!discardInternal || (discardInternal && !host.equals(refHost)))) {
            result.add(refUrl);
          }

          curPos = endPos;
          gotOne = true;
          foundOne = true;
        }
      }

      if (!gotOne) break;
    }

    return foundOne;
  }


  public static final void main(String[] args) {
    DetailedUrl prevUrl = null;

    for (String arg : args) {
      final DetailedUrl dUrl = new DetailedUrl(arg);

      System.out.println(arg + "\n");
      System.out.println("  normalized: " + dUrl.getNormalizedUrl() + "\n");
      System.out.println("    protocol: " + dUrl.getProtocol(true));
      System.out.println("  hostPrefix: " + dUrl.getHostPrefix(true));
      System.out.println("        host: " + dUrl.getHost(true, false, false));
      System.out.println("        port: " + dUrl.getPort());
      System.out.println("        path: " + dUrl.getPath(true));
      System.out.println("      target: " + dUrl.getTarget(false));
      System.out.println("   extension: " + dUrl.getTargetExtension(true));
      System.out.println("       query: " + dUrl.getQuery());
      System.out.println("      anchor: " + dUrl.getAnchor());

      final URL url = dUrl.asUrl();
      if (url != null) {
        System.out.println("       asURL: " + dUrl.asUrl());
      }
      else {
        System.out.println("    urlError: " + dUrl.getUrlError());
      }

      if (prevUrl != null) {
        System.out.println("    combined: " + prevUrl.fixHref(dUrl) + " (with prev=" + prevUrl.toString() + ")");
      }

      prevUrl = dUrl;
    }
  }
}
