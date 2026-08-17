package com.zbkj.common.request.jiuzhoukang;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Contract for the encrypted callback body that Sinocare POSTs to every subscription endpoint. */
public class SinocareEnvelopeRequestTest {
    @Test
    public void keepsCiphertextAndSignatureWithoutTransformingThem() {
        SinocareEnvelopeRequest request = new SinocareEnvelopeRequest()
                .setCiphertext("base64-ciphertext")
                .setSignature("base64-signature");

        assertEquals("base64-ciphertext", request.getCiphertext());
        assertEquals("base64-signature", request.getSignature());
    }
}
