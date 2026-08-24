package top.likoslupus.cellulosesz.api.world

public interface WorldService {

    public fun setTime(world: String, time: Long): WorldResult

    public fun setWeather(
        world: String,
        type: WeatherType,
        seconds: Int
    ): WorldResult

}
