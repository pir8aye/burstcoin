package brs.user;

import brs.Block;
import brs.Constants;
import brs.Burst;
import brs.Transaction;
import brs.db.BurstIterator;
import brs.peer.Peer;
import brs.peer.Peers;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigInteger;

public final class GetInitialData extends UserServlet.UserRequestHandler {

  static final GetInitialData instance = new GetInitialData();

  private GetInitialData() {}

  @Override
  JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {

    JSONArray unconfirmedTransactions = new JSONArray();
    JSONArray activePeers = new JSONArray(), knownPeers = new JSONArray(), blacklistedPeers = new JSONArray();
    JSONArray recentBlocks = new JSONArray();

    try (BurstIterator<? extends Transaction> transactions = Burst.getTransactionProcessor().getAllUnconfirmedTransactions()) {
      while (transactions.hasNext()) {
        Transaction transaction = transactions.next();

        JSONObject unconfirmedTransaction = new JSONObject();
        unconfirmedTransaction.put("index", Users.getIndex(transaction));
        unconfirmedTransaction.put("timestamp", transaction.getTimestamp());
        unconfirmedTransaction.put("deadline", transaction.getDeadline());
        unconfirmedTransaction.put("recipient", Convert.toUnsignedLong(transaction.getRecipientId()));
        unconfirmedTransaction.put("amountNQT", transaction.getAmountNQT());
        unconfirmedTransaction.put("feeNQT", transaction.getFeeNQT());
        unconfirmedTransaction.put("sender", Convert.toUnsignedLong(transaction.getSenderId()));
        unconfirmedTransaction.put("id", transaction.getStringId());

        unconfirmedTransactions.add(unconfirmedTransaction);
      }
    }

    for (Peer peer : Peers.getAllPeers()) {

      if (peer.isBlacklisted()) {

        JSONObject blacklistedPeer = new JSONObject();
        blacklistedPeer.put("index", Users.getIndex(peer));
        blacklistedPeer.put("address", peer.getPeerAddress());
        blacklistedPeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
        blacklistedPeer.put("software", peer.getSoftware());
        if (peer.isWellKnown()) {
          blacklistedPeer.put("wellKnown", true);
        }
        blacklistedPeers.add(blacklistedPeer);

      } else if (peer.getState() == Peer.State.NON_CONNECTED) {

        JSONObject knownPeer = new JSONObject();
        knownPeer.put("index", Users.getIndex(peer));
        knownPeer.put("address", peer.getPeerAddress());
        knownPeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
        knownPeer.put("software", peer.getSoftware());
        if (peer.isWellKnown()) {
          knownPeer.put("wellKnown", true);
        }
        knownPeers.add(knownPeer);

      } else {

        JSONObject activePeer = new JSONObject();
        activePeer.put("index", Users.getIndex(peer));
        if (peer.getState() == Peer.State.DISCONNECTED) {
          activePeer.put("disconnected", true);
        }
        activePeer.put("address", peer.getPeerAddress());
        activePeer.put("announcedAddress", Convert.truncate(peer.getAnnouncedAddress(), "-", 25, true));
        activePeer.put("weight", peer.getWeight());
        activePeer.put("downloaded", peer.getDownloadedVolume());
        activePeer.put("uploaded", peer.getUploadedVolume());
        activePeer.put("software", peer.getSoftware());
        if (peer.isWellKnown()) {
          activePeer.put("wellKnown", true);
        }
        activePeers.add(activePeer);
      }
    }

    try (BurstIterator<? extends Block> lastBlocks = Burst.getBlockchain().getBlocks(0, 59)) {
      for (Block block : lastBlocks) {
        JSONObject recentBlock = new JSONObject();
        recentBlock.put("index", Users.getIndex(block));
        recentBlock.put("timestamp", block.getTimestamp());
        recentBlock.put("numberOfTransactions", block.getTransactions().size());
        recentBlock.put("totalAmountNQT", block.getTotalAmountNQT());
        recentBlock.put("totalFeeNQT", block.getTotalFeeNQT());
        recentBlock.put("payloadLength", block.getPayloadLength());
        recentBlock.put("generator", Convert.toUnsignedLong(block.getGeneratorId()));
        recentBlock.put("height", block.getHeight());
        recentBlock.put("version", block.getVersion());
        recentBlock.put("block", block.getStringId());
        recentBlock.put("baseTarget", BigInteger.valueOf(block.getBaseTarget()).multiply(BigInteger.valueOf(100000))
                        .divide(BigInteger.valueOf(Constants.INITIAL_BASE_TARGET)));

        recentBlocks.add(recentBlock);
      }
    }

    JSONObject response = new JSONObject();
    response.put("response", "processInitialData");
    response.put("version", Burst.VERSION);
    if (unconfirmedTransactions.size() > 0) {
      response.put("unconfirmedTransactions", unconfirmedTransactions);
    }
    if (activePeers.size() > 0) {
      response.put("activePeers", activePeers);
    }
    if (knownPeers.size() > 0) {
      response.put("knownPeers", knownPeers);
    }
    if (blacklistedPeers.size() > 0) {
      response.put("blacklistedPeers", blacklistedPeers);
    }
    if (recentBlocks.size() > 0) {
      response.put("recentBlocks", recentBlocks);
    }

    return response;
  }
}
