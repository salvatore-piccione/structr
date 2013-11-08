/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core.graph;


import java.util.Set;
import java.util.logging.Level;
import org.neo4j.graphdb.GraphDatabaseService;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * Graph service command for database operations that need to be wrapped in
 * a transaction. All operations that modify the database need to be executed
 * in a transaction, which can be achieved using the following code:
 * 
 * <pre>
 * Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
 * 
 *	public Object execute() throws FrameworkException {
 *		// do stuff here
 *	}
 * });
 * </pre>
 * 
 * @author Christian Morgner
 */
public class TransactionCommand extends NodeServiceCommand {

	private static final Logger logger                                  = Logger.getLogger(TransactionCommand.class.getName());
	private static final ThreadLocal<TransactionCommand> currentCommand = new ThreadLocal<>();
	private static final ThreadLocal<TransactionReference> transactions = new ThreadLocal<>();
	private static final MultiSemaphore                    semaphore    = new MultiSemaphore();
	
	private ModificationQueue modificationQueue = null;
	private ErrorBuffer errorBuffer             = null;

	/*
	public <T> T execute(StructrTransaction<T> transaction) throws FrameworkException {
		
		boolean topLevel = (transactions.get() == null);
		boolean retry    = true;
		int retryCount   = 0;
		
		if (topLevel) {
			
			T result = null;
			
			while (retry && retryCount++ < 100) {

				// assume success
				retry = false;

				try {
					result = executeInternal(transaction);

				} catch (RetryException rex) {

					logger.log(Level.INFO, "Deadlock encountered, retrying transaction, count {0}", retryCount);

					retry = true;
				}
			}
			
			return result;
			 
		} else {
			
			return executeInternal(transaction);
		}
	}

	private <T> T executeInternal(StructrTransaction<T> transaction) throws FrameworkException {
		
		GraphDatabaseService graphDb    = (GraphDatabaseService) arguments.get("graphDb");
		TransactionReference tx         = transactions.get();
		boolean topLevel                = (tx == null);
		boolean error                   = false;
		boolean deadlock                = false;
		Set<String> synchronizationKeys = null;
		FrameworkException exception    = null;
		T result                        = null;
		
		if (topLevel) {
		
			// start new transaction
			this.modificationQueue = new ModificationQueue();
			this.errorBuffer       = new ErrorBuffer();
			tx                     = new TransactionReference(graphDb.beginTx());
			
			transactions.set(tx);
			currentCommand.set(this);
		}
	
		// execute structr transaction
		try {
		
			result = transaction.execute();
			
			if (topLevel) {

				// 1. do inner callbacks (may cause transaction to fail)
				if (!modificationQueue.doInnerCallbacks(securityContext, errorBuffer)) {

					// create error
					if (transaction.doValidation) {
						throw new FrameworkException(422, errorBuffer);
					}
				}
				
				// 2. fetch all types of entities modified in this tx
				synchronizationKeys = modificationQueue.getSynchronizationKeys();

				// we need to protect the validation and indexing part of every transaction
				// from being entered multiple times in the presence of validators
				// 3. acquire semaphores for each modified type
				try { semaphore.acquire(synchronizationKeys); } catch (InterruptedException iex) { return null; }

				// finally, do validation under the protection of the semaphores for each type
				if (!modificationQueue.doValidation(securityContext, errorBuffer, transaction.doValidation)) {

					// create error
					throw new FrameworkException(422, errorBuffer);
				}
			}
			
		} catch (DeadlockDetectedException ddex) {
			
			tx.failure();
			
			// this block is entered when we first
			// encounter a DeadlockDetectedException
			// => pass on to parent transaction
			deadlock = true;
			error = true;
			
		} catch (RetryException rex) {
			
			tx.failure();

			// this block is entered when we catch the
			// RetryException from a nested transaction
			// => pass on to parent transaction
			deadlock = true;
			error = true;

		} catch (FrameworkException fex) {
			
			tx.failure();
			
			exception = fex;
			error = true;
			
		} catch (Throwable t) {
			
			tx.failure();

			// TODO: add debugging switch!
			t.printStackTrace();

			exception = new FrameworkException(500, t);
			error = true;

			
		} finally {

			// finish toplevel transaction
			if (topLevel) {

				try {
					tx.success();
					tx.finish();
					
				} finally {

					// release semaphores as the transaction is now finished
					semaphore.release(synchronizationKeys);	// careful: this can be null

					// cleanup
					currentCommand.remove();
					transactions.remove();
				}

				// no error, notify entities
				if (!error) {
					modificationQueue.doOuterCallbacks(securityContext);
					modificationQueue.clear();
				}
			}
		}
		
		if (deadlock) {
			throw new RetryException();
		}
		
		// throw actual exception
		if (exception != null && error) {
			throw exception;
		}
		
		return result;
	}
	*/
	
	public void beginTx() {
		
		final GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		TransactionReference tx            = transactions.get();
		
		if (tx == null) {
		
			// start new transaction
			this.modificationQueue = new ModificationQueue();
			this.errorBuffer       = new ErrorBuffer();
			tx                     = new TransactionReference(graphDb.beginTx());
			
			transactions.set(tx);
			currentCommand.set(this);
		}
		
		// increase depth
		tx.begin();
	}
	
	public void commitTx() throws FrameworkException {
		commitTx(true);
	}
	
	public void commitTx(final boolean doValidation) throws FrameworkException {
	
		final TransactionReference tx = transactions.get();
		if (tx != null && tx.isToplevel()) {

			// 1. do inner callbacks (may cause transaction to fail)
			if (!modificationQueue.doInnerCallbacks(securityContext, errorBuffer)) {

				// create error
				if (doValidation) {

					tx.failure();

					throw new FrameworkException(422, errorBuffer);
				}
			}

			// 2. fetch all types of entities modified in this tx
			Set<String> synchronizationKeys = modificationQueue.getSynchronizationKeys();

			// we need to protect the validation and indexing part of every transaction
			// from being entered multiple times in the presence of validators
			// 3. acquire semaphores for each modified type
			try { semaphore.acquire(synchronizationKeys); } catch (InterruptedException iex) { return; }

			// finally, do validation under the protection of the semaphores for each type
			if (!modificationQueue.doValidation(securityContext, errorBuffer, doValidation)) {

				tx.failure();

				// release semaphores as the transaction is now finished
				semaphore.release(synchronizationKeys);	// careful: this can be null

				// create error
				throw new FrameworkException(422, errorBuffer);
			}

			tx.success();

			// release semaphores as the transaction is now finished
			semaphore.release(synchronizationKeys);	// careful: this can be null
		}
	}
	
	public void finishTx() {
		
		final TransactionReference tx = transactions.get();
		
		if (tx != null) {
			
			if (tx.isToplevel()) {

				// cleanup
				currentCommand.remove();
				transactions.remove();

				tx.finish();

				if (modificationQueue != null) {
					
					modificationQueue.doOuterCallbacks(securityContext);
					modificationQueue.clear();
				}
				
			} else {
				
				tx.end();
			}
			
/*
 *			FIXME:
 *			The problem here is that the call to finish causes the SynchronizationController
 *			to be activated, this triggers a JSON rendering which calls getThumbnail() which
 *			triggers thumbnail generation, all while the current transaction finishes.
 *			
 *			This causes the current (finishing) transaction to be "forgotten" because a new
 *			nested transaction is started IN THE SAME THREAD. This leads to the inner TX to
 *			be executed and finished, and the finish() call to the outer TX never happens
 *			because the inner has overwritten the ThreadLocal.
 * 
 *			We can either prevent any modifications from happening in the SynchronizationController
 *			context or we need to move all code from the synchronization controller into the outer
 *			callback methods.
 * 
 * 
 */
			
			
			
			
			
			
			
		} else {

			System.out.println("###################################################################################################################: transaction without reference");
			Thread.dumpStack();
		}
	}
	
	public static void nodeCreated(NodeInterface node) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.create(node);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Node created while outside of transaction!");
		}
	}
	
	public static void nodeModified(AbstractNode node, PropertyKey key, Object previousValue) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.modify(node, key, previousValue);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Node deleted while outside of transaction!");
		}
	}
	
	public static void nodeDeleted(NodeInterface node) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.delete(node);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Node deleted while outside of transaction!");
		}
	}
	
	public static void relationshipCreated(RelationshipInterface relationship) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.create(relationship);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Relationships created while outside of transaction!");
		}
	}
	
	public static void relationshipModified(RelationshipInterface relationship, PropertyKey key, Object value) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.modify(relationship, null, null);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Relationship deleted while outside of transaction!");
		}
	}
	
	public static void relationshipDeleted(RelationshipInterface relationship, boolean passive) {
		
		TransactionCommand command = currentCommand.get();
		if (command != null) {
			
			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {
				
				modificationQueue.delete(relationship, passive);
				
			} else {
				
				logger.log(Level.SEVERE, "Got empty changeSet from command!");
			}
			
		} else {
			
			logger.log(Level.SEVERE, "Relationship deleted while outside of transaction!");
		}
	}
	
	public static boolean inTransaction() {
		return currentCommand.get() != null;
	}

	private ModificationQueue getModificationQueue() {
		return modificationQueue;
	}
}
