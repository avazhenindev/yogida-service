package com.yogida.meditation.service;

import com.yogida.meditation.config.r2.R2Properties;
import com.yogida.meditation.repository.S3ObjectRepository;
import com.yogida.meditation.service.api.AdminStorageApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Covers the rollback compensation for staged uploads.
 *
 * <p>The asymmetry between the two outcomes is the whole point, and getting it wrong is worse
 * than not having it: compensating on a commit would delete an object a committed row depends on.
 */
@ExtendWith(MockitoExtension.class)
class S3ObjectServiceRollbackTest {

    private static final String BUCKET = "audio";
    private static final String KEY = "media/abc-track.mp3";

    @Mock private S3ObjectRepository s3ObjectRepository;
    @Mock private AdminStorageApi adminStorageApi;
    @Mock private R2Properties r2Properties;

    @AfterEach
    void clearSynchronizations() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private S3ObjectService service() {
        return new S3ObjectService(s3ObjectRepository, adminStorageApi, r2Properties);
    }

    @Test
    @DisplayName("a rolled-back transaction removes the staged object")
    void rollbackDeletesTheStagedObject() {
        TransactionSynchronizationManager.initSynchronization();
        service().deleteObjectOnRollback(BUCKET, KEY);

        // Nothing happens until the transaction completes.
        verifyNoInteractions(adminStorageApi);

        fireCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(adminStorageApi).deleteObject(BUCKET, KEY);
    }

    @Test
    @DisplayName("a committed transaction keeps the object")
    void commitKeepsTheObject() {
        TransactionSynchronizationManager.initSynchronization();
        service().deleteObjectOnRollback(BUCKET, KEY);

        fireCompletion(TransactionSynchronization.STATUS_COMMITTED);
        verify(adminStorageApi, never()).deleteObject(BUCKET, KEY);
    }

    /**
     * STATUS_UNKNOWN means the outcome could not be determined — the commit may well have
     * succeeded. Deleting here would destroy the object belonging to a live row, so the only
     * safe behaviour is to leave it and let the reconciler report it.
     */
    @Test
    @DisplayName("an unknown outcome keeps the object rather than guessing")
    void unknownOutcomeKeepsTheObject() {
        TransactionSynchronizationManager.initSynchronization();
        service().deleteObjectOnRollback(BUCKET, KEY);

        fireCompletion(TransactionSynchronization.STATUS_UNKNOWN);
        verify(adminStorageApi, never()).deleteObject(BUCKET, KEY);
    }

    @Test
    @DisplayName("registering twice deletes once")
    void compensationIsIdempotent() {
        TransactionSynchronizationManager.initSynchronization();
        S3ObjectService service = service();
        service.deleteObjectOnRollback(BUCKET, KEY);

        fireCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        fireCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(adminStorageApi).deleteObject(BUCKET, KEY);
    }

    @Test
    @DisplayName("with no transaction active, nothing is registered")
    void withoutATransactionNothingIsRegistered() {
        service().deleteObjectOnRollback(BUCKET, KEY);
        verifyNoInteractions(adminStorageApi);
    }

    private static void fireCompletion(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(status));
    }
}
