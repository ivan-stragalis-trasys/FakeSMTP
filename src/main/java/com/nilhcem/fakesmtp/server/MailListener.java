package com.nilhcem.fakesmtp.server;

import com.nilhcem.fakesmtp.model.UIModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.helper.SimpleMessageListener;

/**
 * Listens to incoming emails and redirects them to the {@code MailSaver} object.
 *
 * @author Nilhcem
 * @since 1.0
 */
public final class MailListener implements SimpleMessageListener {
	private final MailSaver saver;

	private static final Logger LOGGER = LoggerFactory.getLogger(MailListener.class);

	/**
	 * Creates the listener.
	 *
	 * @param saver a {@code MailServer} object used to save emails and notify components.
	 */
	public MailListener(MailSaver saver) {
		this.saver = saver;
	}

	private final static String BLOCK_DOMAIN = "block.com";
	private final static int BLOCK_SECONDS = 60;

	/**
	 * Accepts all kind of email <i>(always return true)</i>.
	 * <p>
	 * Called once for every RCPT TO during a SMTP exchange.<br>
     * Each accepted recipient will result in a separate deliver() call later.
     * </p>
     *
	 * @param from the user who send the email.
	 * @param recipient the recipient of the email.
	 * @return always return {@code true}
	 */
	public boolean accept(String from, String recipient) {
		handleBlocked(recipient);
		handleRelay(recipient);
		return true;
	}

    /**
     * Receives emails and forwards them to the {@link MailSaver} object.
     */
	@Override
	public void deliver(String from, String recipient, InputStream data) throws IOException {
		saver.saveEmailAndNotify(from, recipient, data);
	}

	private void handleRelay(String recipient) {
		List<String> relayDomains = UIModel.INSTANCE.getRelayDomains();

		if (relayDomains != null) {
			boolean matches = false;
			for (String domain : relayDomains) {
				if (recipient.endsWith(domain)) {
					matches = true;
					break;
				}
			}

			if (!matches) {
				LOGGER.debug("Destination {} doesn't match relay domains", recipient);
				throw new RejectException(550, "5.7.54 SMTP; Unable to relay recipient in non-accepted domain");
			}
		}
	}

	private void handleBlocked(String recipient) {
		if (recipient.endsWith(BLOCK_DOMAIN)) {
			try {
				TimeUnit.SECONDS.sleep(BLOCK_SECONDS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
