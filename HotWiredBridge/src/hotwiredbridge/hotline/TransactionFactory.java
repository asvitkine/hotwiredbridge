package hotwiredbridge.hotline;

public class TransactionFactory {
	private int nextRequestTaskNumber;

	public TransactionFactory() {
		nextRequestTaskNumber = 1;
	}

	public Transaction createRequest(int id, boolean isError) {
		int taskNumber;
		synchronized (this) {
			taskNumber = nextRequestTaskNumber++;
		}
		Transaction t = new Transaction(Transaction.REQUEST, id, taskNumber, isError);
		return t;
	}

	public Transaction createRequest(int id) {
		return createRequest(id, false);
	}
	
	public Transaction createReply(Transaction to, boolean isError) {
		return new Transaction(Transaction.REPLY, 0, to.getTaskNumber(), isError);
	}

	public Transaction createReply(Transaction to) {
		return createReply(to, false);
	}
}
