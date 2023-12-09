package africa.jopen.ripple.utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageQueue {
	private static final MessageQueue INSTANCE = new MessageQueue();
	
	private Queue<String> queue = new ConcurrentLinkedQueue<>();
	
	/**
	 * Return singleton instance of this class.
	 *
	 * @return Singleton.
	 */
	public static MessageQueue instance() {
		return INSTANCE;
	}
	
	private MessageQueue() {
	}
	
	/**
	 * Push string on stack.
	 *
	 * @param s String to push.
	 */
	public void push(String s) {
		queue.add(s);
	}
	
	/**
	 * Pop string from stack.
	 *
	 * @return The string or {@code null}.
	 */
	public String pop() {
		return queue.poll();
	}
	
	/**
	 * Check if stack is empty.
	 *
	 * @return Outcome of test.
	 */
	public boolean isEmpty() {
		return queue.isEmpty();
	}
	
	/**
	 * Peek at stack without changing it.
	 *
	 * @return String peeked or {@code null}.
	 */
	public String peek() {
		return queue.peek();
	}
}
