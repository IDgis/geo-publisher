package nl.idgis.publisher.database;

import java.util.Optional;

public interface AsyncTransactional {

	Optional<AsyncTransactionRef> getTransactionRef();
}
