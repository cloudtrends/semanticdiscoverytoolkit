package org.sd.cluster.io;


import java.util.List;

/**
 * JMX MXBean for the cluster SafeDepositBox.
 * <p>
 * @author Spence Koehler
 */
public interface SafeDepositBoxMXBean {

	/**
	 * Get the total number of drawers (active and incinerated).
	 */
	public long getTotalDrawerCount();

	/**
	 * Get the number of incinerated drawers.
	 */
	public long getNumIncineratedDrawers();

	/**
	 * Get the number of active drawers (filled or filling).
	 */
	public long getNumActiveDrawers();

	/**
	 * Get the number of active drawers that are filled.
	 */
	public long getNumFilledDrawers();

	/**
	 * Get the number of active drawers that are yet to be filled.
	 */
	public long getNumFillingDrawers();

	/**
	 * Get the keys for the filled drawers.
	 */
	public List<String> getFilledKeys();

	/**
	 * Get the contents.toString from the drawer with the given key.
	 */
	public String getContentsString(String key);

	/**
	 * Incinerate the drawer with the given key.
	 */
	public void incinerate(String key);

	/**
	 * Incinerate all drawers that are older than the given age (in millis).
	 *
	 * @return the number of drawers incinerated.
	 */
	public long incinerateOlder(long age);

}
