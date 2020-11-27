/**
 * This source file is part of Cell.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cell.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可缓存的队列执行器。
 */
public final class CachedQueueExecutor implements ExecutorService {

	private ExecutorService executor;

	/** 最大并发线程数量。 */
	private int maxThreads = 8;

	private AtomicInteger numThreads = new AtomicInteger(0);

	/** 执行队列。 */
	private ConcurrentLinkedQueue<Runnable> queue;

	/** 任务池。 */
	private ConcurrentLinkedQueue<QueueTask> taskPool;

	/**
	 * 构造函数。
	 *
	 * @param maxThreads 指定最大并发线程数量。
	 */
	private CachedQueueExecutor(int maxThreads) {
		this.executor = Executors.newCachedThreadPool();
		this.maxThreads = maxThreads;
		this.queue = new ConcurrentLinkedQueue<Runnable>();
		this.taskPool = new ConcurrentLinkedQueue<QueueTask>();
	}

	/**
	 * 创建可缓存队列执行器。
	 *
	 * @param maxThreads 指定最大并发线程数量。
	 * @return 返回 {@link CachedQueueExecutor} 的实例。
	 */
	public static CachedQueueExecutor newCachedQueueThreadPool(int maxThreads) {
		if (maxThreads <= 0) {
			throw new IllegalArgumentException("Max threads is not less than zero.");
		}

		return new CachedQueueExecutor(maxThreads);
	}

	/**
	 * 重置最大线程数。
	 *
	 * @param maxThreads 指定新的最大线程数量。
	 */
	public void resetMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(Runnable command) {
		// 命令入队
		this.queue.offer(command);

		if (this.numThreads.get() < this.maxThreads) {
			// 线程计数
			this.numThreads.incrementAndGet();

			this.executor.execute(borrowTask());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return this.executor.awaitTermination(timeout, unit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return this.executor.invokeAll(tasks, timeout, unit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return this.executor.invokeAny(tasks);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
						   TimeUnit unit) throws InterruptedException, ExecutionException,
			TimeoutException {
		return this.executor.invokeAny(tasks, timeout, unit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isShutdown() {
		return this.executor.isShutdown();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isTerminated() {
		return this.executor.isTerminated();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		this.queue.clear();
		this.executor.shutdown();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Runnable> shutdownNow() {
		this.queue.clear();
		return this.executor.shutdownNow();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return this.executor.submit(task);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Future<?> submit(Runnable task) {
		return this.executor.submit(task);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return this.executor.submit(task, result);
	}

	/**
	 * 执行队列任务。
	 */
	protected final class QueueTask implements Runnable {
		protected QueueTask() {
		}

		@Override
		public void run() {
			do {
				Runnable task = queue.poll();
				if (null != task) {
					task.run();
				}
			} while (!queue.isEmpty());

			numThreads.decrementAndGet();

			returnTask(this);
		}
	}

	private QueueTask borrowTask() {
		QueueTask task = this.taskPool.poll();
		if (null == task) {
			return new QueueTask();
		}

		return task;
	}

	private void returnTask(QueueTask task) {
		this.taskPool.offer(task);
	}
}
