package bbejeck.chapter_6.transformer;


import bbejeck.chapter_6.punctuator.StockPerformancePunctuator;
import bbejeck.model.StockPerformance;
import bbejeck.model.StockTransaction;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Instant;

public class StockPerformanceTransformer implements Transformer<String, StockTransaction, KeyValue<String, StockPerformance>> {

    private String stateStoreName;
    private double differentialThreshold;
    private KeyValueStore<String, StockPerformance> keyValueStore;


    public StockPerformanceTransformer(String stateStoreName, double differentialThreshold) {
        this.stateStoreName = stateStoreName;
        this.differentialThreshold = differentialThreshold;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(ProcessorContext processorContext) {
        keyValueStore = (KeyValueStore) processorContext.getStateStore(stateStoreName);
        StockPerformancePunctuator punctuator = new StockPerformancePunctuator(differentialThreshold, processorContext, keyValueStore);
        processorContext.schedule(15000, PunctuationType.STREAM_TIME, punctuator);
    }

    @Override
    public KeyValue<String, StockPerformance> transform(String symbol, StockTransaction transaction) {
        if (symbol != null) {
            StockPerformance stockPerformance = keyValueStore.get(symbol);

            if (stockPerformance == null) {
                stockPerformance = new StockPerformance();
            }

            stockPerformance.updatePriceStats(transaction.getSharePrice());
            stockPerformance.updateVolumeStats(transaction.getShares());
            stockPerformance.setLastUpdateSent(Instant.now());

            keyValueStore.put(symbol, stockPerformance);
        }
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public KeyValue<String, StockPerformance> punctuate(long l) {
        throw new UnsupportedOperationException("Should use the punctuate method on Punctuator");
    }

    @Override
    public void close() {
        //no-op
    }

}

