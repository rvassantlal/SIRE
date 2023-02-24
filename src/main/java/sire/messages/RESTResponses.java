/*
 * Copyright 2023 Tiago Carvalho
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sire.messages;

public class RESTResponses {
    public static class preJoinResponse {
        String pubKey;
        String timestamp;
        String sigma;
        String signingPublicKey;
        String randomPublicKey;

        public preJoinResponse(String pubKey, String timestamp, String sigma, String signingPublicKey, String randomPublicKey) {
            this.pubKey = pubKey;
            this.timestamp = timestamp;
            this.sigma = sigma;
            this.signingPublicKey = signingPublicKey;
            this.randomPublicKey = randomPublicKey;
        }

        public String getPubKey() {
            return pubKey;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getSigma() {
            return sigma;
        }

        public String getSigningPublicKey() {
            return signingPublicKey;
        }

        public String getRandomPublicKey() {
            return randomPublicKey;
        }
    }

    public static class JoinResponse {
        String pubKey;
        String timestamp;
        String hash;
        String sigma;
        String signingPublicKey;
        String randomPublicKey;

        public JoinResponse(String pubKey, String timestamp, String hash, String sigma, String signingPublicKey,
                            String randomPublicKey) {
            this.pubKey = pubKey;
            this.timestamp = timestamp;
            this.hash = hash;
            this.sigma = sigma;
            this.signingPublicKey = signingPublicKey;
            this.randomPublicKey = randomPublicKey;
        }

        public String getPubKey() {
            return pubKey;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getHash() {
            return hash;
        }

        public String getSigma() {
            return sigma;
        }

        public String getSigningPublicKey() {
            return signingPublicKey;
        }

        public String getRandomPublicKey() {
            return randomPublicKey;
        }
    }
}
