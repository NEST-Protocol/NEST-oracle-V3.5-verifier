package com.nest.ib.contract;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.generated.*;

import java.math.BigInteger;

public class PriceSheetPub2 extends StaticStruct {

    public PriceSheetPub2(BigInteger miner,
                          BigInteger height,
                          BigInteger ethNum,
                          BigInteger remainNum,

                          BigInteger level,
                          BigInteger typ,
                          BigInteger state,
                          BigInteger index,
                          BigInteger nestNum1k,
                          BigInteger tokenAmountPerEth) {
        super(new Uint160(miner),
                new Uint32(height),
                new Uint32(ethNum),
                new Uint32(remainNum),

                new Uint8(level),
                new Uint8(typ),
                new Uint8(state),
                new Uint256(index),
                new Uint32(nestNum1k),
                new Uint128(tokenAmountPerEth));

        this.miner =new Address(miner);
        this.height = height;
        this.ethNum = ethNum;
        this.remainNum = remainNum;

        this.level = level;
        this.typ = typ;
        this.state = state;
        this.index = index;
        this.nestNum1k = nestNum1k;
        this.tokenAmountPerEth = tokenAmountPerEth;
    }

    public PriceSheetPub2(Uint160 miner,
                          Uint32 height,
                          Uint32 ethNum,
                          Uint32  remainNum,

                          Uint8 level,
                          Uint8 typ,
                          Uint8 state,
                          Uint256 index,
                          Uint32 nestNum1k,
                          Uint128 tokenAmountPerEth) {
        super(  miner,
                height,
                ethNum,
                remainNum,

                level,
                typ,
                state,
                index,
                nestNum1k,
                tokenAmountPerEth);

        this.miner = new Address(miner);
        this.height = height.getValue();
        this.ethNum = ethNum.getValue();
        this.remainNum = remainNum.getValue();

        this.level = level.getValue();
        this.typ = typ.getValue();
        this.state = state.getValue();
        this.index = index.getValue();
        this.nestNum1k = nestNum1k.getValue();
        this.tokenAmountPerEth = tokenAmountPerEth.getValue();
    }

    public Address miner;
    public BigInteger height;
    public BigInteger ethNum;
    public BigInteger remainNum;
    public BigInteger level;
    public BigInteger typ;
    public BigInteger state;
    public BigInteger index;
    public BigInteger nestNum1k;
    public BigInteger tokenAmountPerEth;

    public Address getMiner() {
        return miner;
    }

    public void setMiner(Address miner) {
        this.miner = miner;
    }

    public BigInteger getHeight() {
        return height;
    }

    public void setHeight(BigInteger height) {
        this.height = height;
    }

    public BigInteger getEthNum() {
        return ethNum;
    }

    public void setEthNum(BigInteger ethNum) {
        this.ethNum = ethNum;
    }

    public BigInteger getTyp() {
        return typ;
    }

    public void setTyp(BigInteger typ) {
        this.typ = typ;
    }

    public BigInteger getState() {
        return state;
    }

    public void setState(BigInteger state) {
        this.state = state;
    }

    public BigInteger getRemainNum() {
        return remainNum;
    }

    public void setRemainNum(BigInteger remainNum) {
        this.remainNum = remainNum;
    }

    public BigInteger getLevel() {
        return level;
    }

    public void setLevel(BigInteger level) {
        this.level = level;
    }

    public BigInteger getIndex() {
        return index;
    }

    public void setIndex(BigInteger index) {
        this.index = index;
    }

    public BigInteger getNestNum1k() {
        return nestNum1k;
    }

    public void setNestNum1k(BigInteger nestNum1k) {
        this.nestNum1k = nestNum1k;
    }

    public BigInteger getTokenAmountPerEth() {
        return tokenAmountPerEth;
    }

    public void setTokenAmountPerEth(BigInteger tokenAmountPerEth) {
        this.tokenAmountPerEth = tokenAmountPerEth;
    }

}
