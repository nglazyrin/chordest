package msdchallenge.repository;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;

public interface IRepositoryWrapper {

	public abstract Repository getRepository();

	public abstract RepositoryConnection getRepositoryConnection();

	public abstract void shutdown();

}