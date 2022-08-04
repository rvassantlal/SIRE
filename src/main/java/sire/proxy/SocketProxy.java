package sire.proxy;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import confidential.client.ConfidentialServiceProxy;
import confidential.client.Response;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.math.ec.ECPoint;
import sire.membership.DeviceContext;
import sire.serverProxyUtils.*;

import static sire.messages.ProtoUtils.*;

import sire.messages.Messages.*;
import sire.schnorr.SchnorrSignatureScheme;
import vss.facade.SecretSharingException;
import javax.crypto.*;
import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

/**
 * @author robin
 */

public class SocketProxy implements Runnable {
	private static final int AES_KEY_LENGTH = 128;
	private final ConfidentialServiceProxy serviceProxy;
	private final MessageDigest messageDigest;
	private final ECPoint verifierPublicKey;
	private final SchnorrSignatureScheme signatureScheme;
/*	private final Map<String, AttesterContext> attesters;*/
	private final SecureRandom rndGenerator = new SecureRandom("sire".getBytes());
	private final CMac macEngine;
/*	private final SecretKeyFactory secretKeyFactory;
	private final ECPoint curveGenerator;
	private final Cipher symmetricCipher;*/
	private final int proxyId;
	private final Object proxyLock;

	public SocketProxy(int proxyId) throws SireException{
		System.out.println("Proxy start!");
		this.proxyId = proxyId;

		try {
			ServersResponseHandlerWithoutCombine responseHandler = new ServersResponseHandlerWithoutCombine();
			serviceProxy = new ConfidentialServiceProxy(proxyId, responseHandler);
			proxyLock = new Object();
		} catch (SecretSharingException e) {
			throw new SireException("Failed to contact the distributed verifier", e);
		}
		System.out.println("Connection established!");
		try {
			messageDigest = MessageDigest.getInstance("SHA256");
			BlockCipher aes = new AESEngine();

			macEngine = new CMac(aes);
			//secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			signatureScheme = new SchnorrSignatureScheme();
			//curveGenerator = signatureScheme.getGenerator();
			//symmetricCipher = Cipher.getInstance("AES/GCM/NoPadding");
		} catch (NoSuchAlgorithmException e) {
			throw new SireException("Failed to initialize cryptographic tools", e);
		}
		Response response;
		try {
			ProxyMessage msg = ProxyMessage.newBuilder()
					.setOperation(ProxyMessage.Operation.ATTEST_GENERATE_SIGNING_KEY)
					.build();
			byte[] b = msg.toByteArray();
			response = serviceProxy.invokeOrdered(b);//new byte[]{(byte) Operation.GENERATE_SIGNING_KEY.ordinal()});
		} catch (SecretSharingException e) {
			throw new SireException("Failed to obtain verifier's public key", e);
		}
		verifierPublicKey = signatureScheme.decodePublicKey(response.getPainData());

		//attesters = new HashMap<>();
	}

	@Override
	public void run() {
		try {
			ServerSocket ss = new ServerSocket(2500 + this.proxyId);
			Socket s;
			while(true) {
				s = ss.accept();
				System.out.println("New client!");
				new SireProxyThread(s).start();
				System.out.println("Connection accepted");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class SireProxyThread extends Thread {

		private final Socket s;

		public SireProxyThread(Socket s) {
			this.s = s;
			System.out.println("Proxy Thread started!");
		}
		@Override
		public void run() {
			try {
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(s.getInputStream());

				while (!s.isClosed()) {
					//System.out.println("Running!");
					Object o;
					while ((o = ois.readObject()) != null) {
						System.out.println("Object received! " + o);
						if (o instanceof ProxyMessage msg) {
							switch(msg.getOperation()) {
								case ATTEST_GET_VERIFIER_PUBLIC_KEY -> oos.writeObject(SchnorrSignatureScheme.encodePublicKey(verifierPublicKey));
								default -> {
									ProxyResponse result = runProxyMessage(msg);
									if(result != null)
										oos.writeObject(result);
								}
							}
						}

					}
				}
			} catch (IOException | ClassNotFoundException | SecretSharingException | SireException e) {
				//e.printStackTrace();
			}
		}

		private ProxyResponse runProxyMessage(ProxyMessage msg) throws IOException, SecretSharingException, ClassNotFoundException, SireException {
			Response res;
			if(msg.getOperation().toString().contains("GET") || msg.getOperation().toString().contains("VIEW"))
				res = serviceProxy.invokeUnordered(msg.toByteArray());
			else {
				synchronized (proxyLock) {
					res = serviceProxy.invokeOrdered(msg.toByteArray());
				}
			}

			return switch(msg.getOperation()) {
				case MAP_GET -> mapGet(res);
				case MAP_LIST -> mapList(res);
				case MEMBERSHIP_VIEW -> memberView(res);
				case EXTENSION_GET -> extGet(res);
				case POLICY_GET -> policyGet(res);
				case TIMESTAMP_GET -> timestampGet(res);
				case TIMESTAMP_ATT -> timestampAtt(res);
				case MEMBERSHIP_JOIN -> join(res);
				default -> null;
			};
		}

		private ProxyResponse join(Response res) throws SecretSharingException, InvalidProtocolBufferException {
			ProxyResponse proxyResponse = ProxyResponse.parseFrom(res.getPainData());
			return proxyResponse;
		}

		private ProxyResponse timestampAtt(Response res) throws SireException, SecretSharingException, IOException, ClassNotFoundException {
			/*UncombinedConfidentialResponse signatureResponse;
			try {
				synchronized (proxyLock) {
					signatureResponse = (UncombinedConfidentialResponse) serviceProxy.invokeOrdered2(msg.toByteArray());
				}
			} catch (SecretSharingException e) {
				throw new SireException("Verifier failed to sign", e);
			}
			byte[] data = signatureResponse.getPlainData();
			System.out.println(Arrays.toString(data) + "\n" + data.length);

			PublicPartialSignature partialSignature;
			try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
				 ObjectInput in = new ObjectInputStream(bis)) {
				partialSignature = PublicPartialSignature.deserialize(signatureScheme, in);
			} catch (IOException | ClassNotFoundException e) {
				throw new SireException("Failed to deserialize public data of partial signatures");
			}

			EllipticCurveCommitment signingKeyCommitment = partialSignature.getSigningKeyCommitment();
			EllipticCurveCommitment randomKeyCommitment = partialSignature.getRandomKeyCommitment();
			ECPoint randomPublicKey = partialSignature.getRandomPublicKey();
			VerifiableShare[] verifiableShares = signatureResponse.getVerifiableShares()[0];
			Share[] partialSignatures = new Share[verifiableShares.length];
			for (int i = 0; i < verifiableShares.length; i++) {
				partialSignatures[i] = verifiableShares[i].getShare();
			}

			if (randomKeyCommitment == null)
				throw new IllegalStateException("Random key commitment is null");

			try {
				BigInteger sigma = signatureScheme.combinePartialSignatures(
						serviceProxy.getCurrentF(),
						data,
						signingKeyCommitment,
						randomKeyCommitment,
						randomPublicKey,
						partialSignatures
				);
				SchnorrSignature sign = new SchnorrSignature(sigma.toByteArray(), verifierPublicKey.getEncoded(true),
						randomPublicKey.getEncoded(true));
				return ProxyResponse.newBuilder()
						.setTimestamp(ByteString.copyFrom(Arrays.copyOfRange(data, 0, 12)))
						.setPubKey(ByteString.copyFrom(Arrays.copyOfRange(data, 12, data.length)))
						.setSign(schnorrToProto(sign))
						.build();
			} catch (SecretSharingException e) {
				e.printStackTrace();
			}
			return null;*/

			ProxyResponse proxyResponse = ProxyResponse.parseFrom(res.getPainData());
			return proxyResponse;
		}

		private ProxyResponse timestampGet(Response res) {
			return null;
		}

		private ProxyResponse policyGet(Response res) throws IOException, ClassNotFoundException, SecretSharingException {
			byte[] tmp = res.getPainData();
			if (tmp != null) {
				return ProxyResponse.newBuilder()
						.setType(ProxyResponse.ResponseType.POLICY_GET)
						.setPolicy((String) deserialize(tmp))
						.build();
			} else {
				return ProxyResponse.newBuilder().build();
			}

		}

		private ProxyResponse extGet(Response res) throws IOException, ClassNotFoundException, SecretSharingException {
			byte[] tmp = res.getPainData();
			if (tmp != null) {
				return ProxyResponse.newBuilder()
						.setType(ProxyResponse.ResponseType.EXTENSION_GET)
						.setExtension((String) deserialize(tmp))
						.build();
			} else {
				return ProxyResponse.newBuilder().build();
			}
		}

		private ProxyResponse memberView(Response res) throws IOException, ClassNotFoundException, SecretSharingException {
			byte[] tmp = res.getPainData();
			ProxyResponse.Builder prBuilder = ProxyResponse.newBuilder();
			if (tmp != null) {
				ByteArrayInputStream bin = new ByteArrayInputStream(tmp);
				ObjectInputStream oin = new ObjectInputStream(bin);
				List<DeviceContext> members = (List<DeviceContext>) oin.readObject();
				for (DeviceContext d : members)
					if(d.isCertificateValid()) {
						prBuilder.addMembers(ProxyResponse.ProtoDeviceContext.newBuilder()
								.setDeviceId(d.getDeviceId())
								.setTime(Timestamp.newBuilder()
										.setSeconds(d.getLastPing().getTime() / 1000)
										.build())
								.setCertExpTime(Timestamp.newBuilder()
										.setSeconds(d.getCertExpTime().getTime() / 1000)
										.build())
								.build());
					} else {
						prBuilder.addMembers(ProxyResponse.ProtoDeviceContext.newBuilder()
								.setDeviceId(d.getDeviceId())
								.setTime(Timestamp.newBuilder()
										.setSeconds(d.getLastPing().getTime() / 1000)
										.build())
								.build());
					}

			}
			return prBuilder.build();
		}

		private ProxyResponse mapList(Response res) throws IOException, ClassNotFoundException, SecretSharingException {
			byte[] tmp = res.getPainData();
			ProxyResponse.Builder prBuilder = ProxyResponse.newBuilder();
			if (tmp != null) {
				ByteArrayInputStream bin = new ByteArrayInputStream(tmp);
				ObjectInputStream oin = new ObjectInputStream(bin);
				ArrayList<byte[]> lst = (ArrayList<byte[]>) oin.readObject();
				for (byte[] b : lst)
					prBuilder.addList(ByteString.copyFrom(b));
			}
			return prBuilder.build();
		}

		private ProxyResponse mapGet(Response res) throws SecretSharingException {
			byte[] tmp = res.getPainData();
			if (tmp != null) {
				return ProxyResponse.newBuilder()
						.setValue(ByteString.copyFrom(tmp))
						.build();
			} else {
				return ProxyResponse.newBuilder().build();
			}
		}


		/*private SecretKey createSecretKey(char[] password, byte[] salt) throws InvalidKeySpecException {
			KeySpec spec = new PBEKeySpec(password, salt, 65536, AES_KEY_LENGTH);
			return new SecretKeySpec(secretKeyFactory.generateSecret(spec).getEncoded(), "AES");
		}

		private byte[] encryptData(SecretKey key, byte[] data) throws SireException {
			try {
				symmetricCipher.init(Cipher.ENCRYPT_MODE, key);
				return symmetricCipher.doFinal(data);
			} catch (InvalidKeyException | IllegalBlockSizeException
					| BadPaddingException e) {
				throw new SireException("Failed to encrypt data", e);
			}
		}

		private boolean verifyMac(byte[] secretKey, byte[] mac, byte[]... contents) {
			return Arrays.equals(computeMac(secretKey, contents), mac);
		}

		private SchnorrSignature getSignatureFromVerifier(byte[] data) throws SireException {

			ProxyMessage signingRequest = ProxyMessage.newBuilder()
					.setOperation(ProxyMessage.Operation.ATTEST_SIGN_DATA)
					.setDataToSign(ByteString.copyFrom(data))
					.build();
			UncombinedConfidentialResponse signatureResponse;
			try {
				synchronized (proxyLock) {
					signatureResponse = (UncombinedConfidentialResponse) serviceProxy.invokeOrdered2(signingRequest.toByteArray());
				}
			} catch (SecretSharingException e) {
				throw new SireException("Verifier failed to sign", e);
			}

			PublicPartialSignature partialSignature;
			try (ByteArrayInputStream bis = new ByteArrayInputStream(signatureResponse.getPlainData());
				 ObjectInput in = new ObjectInputStream(bis)) {
				partialSignature = PublicPartialSignature.deserialize(signatureScheme, in);
			} catch (IOException | ClassNotFoundException e) {
				throw new SireException("Failed to deserialize public data of partial signatures");
			}

			EllipticCurveCommitment signingKeyCommitment = partialSignature.getSigningKeyCommitment();
			EllipticCurveCommitment randomKeyCommitment = partialSignature.getRandomKeyCommitment();
			ECPoint randomPublicKey = partialSignature.getRandomPublicKey();
			VerifiableShare[] verifiableShares = signatureResponse.getVerifiableShares()[0];
			Share[] partialSignatures = new Share[verifiableShares.length];
			for (int i = 0; i < verifiableShares.length; i++) {
				partialSignatures[i] = verifiableShares[i].getShare();
			}

			if (randomKeyCommitment == null)
				throw new IllegalStateException("Random key commitment is null");

			try {
				BigInteger sigma = signatureScheme.combinePartialSignatures(
						serviceProxy.getCurrentF(),
						data,
						signingKeyCommitment,
						randomKeyCommitment,
						randomPublicKey,
						partialSignatures
				);
				return new SchnorrSignature(sigma.toByteArray(), verifierPublicKey.getEncoded(true),
						randomPublicKey.getEncoded(true));
			} catch (SecretSharingException e) {
				throw new SireException("Failed to combine partial signatures", e);
			}

		}*/

		private byte[] computeHash(byte[]... contents) {
			for (byte[] content : contents) {
				messageDigest.update(content);
			}
			return messageDigest.digest();
		}

		private byte[] computeMac(byte[] secretKey, byte[]... contents) {
			macEngine.init(new KeyParameter(secretKey));
			for (byte[] content : contents) {
				macEngine.update(content, 0, content.length);
			}
			byte[] mac = new byte[macEngine.getMacSize()];
			macEngine.doFinal(mac, 0);
			return mac;
		}

		private BigInteger getRandomNumber(BigInteger field) {
			BigInteger rndBig = new BigInteger(field.bitLength() - 1, rndGenerator);
			if (rndBig.compareTo(BigInteger.ZERO) == 0) {
				rndBig = rndBig.add(BigInteger.ONE);
			}

			return rndBig;
		}

		private void close() {
			synchronized (proxyLock) {
				serviceProxy.close();
			}
		}
	}
}
