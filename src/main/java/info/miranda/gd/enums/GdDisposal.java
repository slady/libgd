package info.miranda.gd.enums;

/**
 * Group: GifAnim
 *
 *   Legal values for Disposal. gdDisposalNone is always used by
 *   the built-in optimizer if previm is passed.
 *
 * Constants: gdImageGifAnim
 *
 *   gdDisposalUnknown              - Not recommended
 *   gdDisposalNone                 - Preserve previous frame
 *   gdDisposalRestoreBackground    - First allocated color of palette
 *   gdDisposalRestorePrevious      - Restore to before start of frame
 *
 * See also: <gdImageGifAnimAdd>
 */
public enum GdDisposal {
	UNKNOWN,
	NONE,
	RESTORE_BACKGROUND,
	RESTORE_PREVIOUS
}
